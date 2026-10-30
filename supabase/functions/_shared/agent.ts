// F2-06: shopping agent. Muse Spark plans which tools to call; the tools compute every number.
// The agent cannot change the list: usulkan_tambah_barang only returns a proposal that the
// person accepts with a button (H5).

import { chatCompletion } from "./meta.ts";
import { RECEIPT_MODEL } from "./receipt.ts";

export interface Proposal {
  label: string;
  quantity: number;
}

export interface ToolExecutors {
  cari_harga(args: { nama: string }): Promise<unknown>;
  hitung_estimasi(args: { persen_cadangan?: number }): Promise<unknown>;
  bandingkan_toko(args: Record<string, never>): Promise<unknown>;
}

export interface ChatTurn {
  role: "user" | "assistant";
  content: string;
}

export interface AgentResult {
  reply: string;
  proposals: Proposal[];
  tool_calls: { name: string; args: unknown }[];
}

const SYSTEM_PROMPT = `Kamu asisten belanja HargaTetangga untuk satu rumah tangga di Kudus. Jawab dalam bahasa Indonesia yang ramah, singkat, dan jelas.

Aturan:
- Setiap angka harga atau rupiah HARUS berasal dari hasil tool di percakapan ini. Jangan pernah menebak atau mengarang harga.
- Saat menyebut harga, sebut tokonya dan umur harganya (misalnya "di Indomaret, 3 hari lalu").
- Kalau barang belum ada data harga, katakan terus terang "belum ada data harga" dan sarankan memotret struk saat membelinya.
- Pertanyaan "bawa uang berapa" atau "habis berapa": panggil hitung_estimasi. "Belanja di mana" atau "toko mana paling murah": panggil bandingkan_toko. Harga satu barang: panggil cari_harga.
- Kamu tidak bisa mengubah daftar belanja. Kalau ada barang yang perlu ditambahkan (misalnya bahan untuk menu yang ingin dimasak), panggil usulkan_tambah_barang sekali untuk setiap barang. Pengguna yang memutuskan lewat tombol. Jangan usulkan bumbu dapur yang biasanya sudah ada kecuali diminta; sebut saja di jawaban sebagai "kalau belum ada di rumah".
- Kamu pencatat dan pembanding harga, bukan penasihat keuangan. Jangan memberi saran investasi, pinjaman, atau kredit.
- Format rupiah seperti "Rp 75.400".
- Tulis teks biasa tanpa markdown (tanpa ** atau #). Daftar boleh memakai "- " di awal baris.
- Jangan menyebut nama tool atau istilah teknis kepada pengguna.`;

const TOOLS = [
  {
    type: "function",
    function: {
      name: "cari_harga",
      description: "Cari harga terakhir sebuah barang di setiap toko dari struk yang sudah diperiksa. Mengembalikan produk yang cocok, harga per toko, tanggal, dan umur harga.",
      parameters: {
        type: "object",
        properties: { nama: { type: "string", description: "Nama barang, misalnya 'minyak goreng' atau 'Indomie goreng'" } },
        required: ["nama"],
      },
    },
  },
  {
    type: "function",
    function: {
      name: "hitung_estimasi",
      description: "Hitung estimasi uang belanja untuk daftar belanja saat ini: total, rentang, uang yang perlu dibawa, toko yang dipakai, dan barang yang belum ada data harga.",
      parameters: {
        type: "object",
        properties: { persen_cadangan: { type: "number", description: "Cadangan uang dalam persen, default 10" } },
      },
    },
  },
  {
    type: "function",
    function: {
      name: "bandingkan_toko",
      description: "Bandingkan biaya daftar belanja saat ini di setiap toko dan kombinasi dua toko, termasuk biaya tambahan per toko.",
      parameters: { type: "object", properties: {} },
    },
  },
  {
    type: "function",
    function: {
      name: "usulkan_tambah_barang",
      description: "Usulkan satu barang untuk ditambahkan ke daftar belanja. Tidak langsung menambah; pengguna menyetujui lewat tombol.",
      parameters: {
        type: "object",
        properties: {
          label: { type: "string", description: "Nama barang, misalnya 'Wortel 500 g'" },
          jumlah: { type: "number", description: "Jumlah unit, default 1" },
        },
        required: ["label"],
      },
    },
  },
];

const MAX_STEPS = 6;

export async function runAgent(
  apiKey: string,
  history: ChatTurn[],
  message: string,
  tools: ToolExecutors,
): Promise<AgentResult> {
  const messages: Record<string, unknown>[] = [
    { role: "developer", content: SYSTEM_PROMPT },
    ...history.slice(-10).map((t) => ({ role: t.role, content: t.content })),
    { role: "user", content: message },
  ];
  const proposals: Proposal[] = [];
  const trace: AgentResult["tool_calls"] = [];

  for (let step = 0; step < MAX_STEPS; step++) {
    const completion = await chatCompletion(apiKey, {
      model: RECEIPT_MODEL,
      reasoning_effort: "low",
      max_completion_tokens: 8000,
      messages,
      tools: TOOLS,
      tool_choice: "auto",
    });
    const msg = completion.choices[0]?.message;
    if (!msg) throw new Error("Empty agent response");

    if (!msg.tool_calls?.length) {
      return { reply: (msg.content ?? "").trim(), proposals, tool_calls: trace };
    }

    messages.push({ role: "assistant", content: msg.content ?? "", tool_calls: msg.tool_calls });
    for (const call of msg.tool_calls) {
      let result: unknown;
      // Arguments come from the model: parse and validate before use.
      let args: Record<string, unknown> = {};
      try {
        args = JSON.parse(call.function.arguments || "{}");
      } catch {
        result = { error: "Argumen bukan JSON yang valid" };
      }
      trace.push({ name: call.function.name, args });
      if (result === undefined) {
        try {
          result = await execute(call.function.name, args, tools, proposals);
        } catch (err) {
          result = { error: (err as Error).message };
        }
      }
      messages.push({ role: "tool", tool_call_id: call.id, content: JSON.stringify(result) });
    }
  }
  return {
    reply: "Maaf, pertanyaan ini butuh terlalu banyak langkah. Coba tanyakan lebih spesifik.",
    proposals,
    tool_calls: trace,
  };
}

async function execute(
  name: string,
  args: Record<string, unknown>,
  tools: ToolExecutors,
  proposals: Proposal[],
): Promise<unknown> {
  switch (name) {
    case "cari_harga": {
      const nama = typeof args.nama === "string" ? args.nama.trim() : "";
      if (nama.length < 2) return { error: "nama wajib diisi" };
      return await tools.cari_harga({ nama: nama.slice(0, 80) });
    }
    case "hitung_estimasi": {
      const pct = Number(args.persen_cadangan);
      return await tools.hitung_estimasi({ persen_cadangan: Number.isFinite(pct) ? Math.min(50, Math.max(0, pct)) : 10 });
    }
    case "bandingkan_toko":
      return await tools.bandingkan_toko({});
    case "usulkan_tambah_barang": {
      const label = typeof args.label === "string" ? args.label.trim().slice(0, 80) : "";
      if (!label) return { error: "label wajib diisi" };
      const qty = Number(args.jumlah);
      if (proposals.length >= 20) return { error: "Terlalu banyak usulan" };
      proposals.push({ label, quantity: Number.isFinite(qty) && qty > 0 ? qty : 1 });
      return { status: "menunggu persetujuan pengguna", label };
    }
    default:
      return { error: `Tool ${name} tidak dikenal` };
  }
}
