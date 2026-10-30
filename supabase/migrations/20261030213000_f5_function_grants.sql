-- F5: household functions run with elevated rights, so only signed-in users may call them.

do $$
declare f text;
begin
  foreach f in array array[
    'my_household_id(boolean)', 'household_peers()', 'create_household(text)',
    'request_join(text)', 'decide_member(uuid, boolean)', 'leave_household()'
  ] loop
    execute format('revoke execute on function public.%s from public, anon', f);
    execute format('grant execute on function public.%s to authenticated', f);
  end loop;
end $$;
