"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { createClient } from "@/lib/supabase/client";

function displayLabel(
  user:
    | { email?: string | null; user_metadata?: { email?: string; name?: string; nickname?: string } }
    | null
    | undefined
) {
  if (!user) return null;
  return (
    user.email ||
    user.user_metadata?.email ||
    user.user_metadata?.name ||
    user.user_metadata?.nickname ||
    "사용자"
  );
}

export default function AuthHeader() {
  const router = useRouter();
  const [label, setLabel] = useState<string | null>(null);

  useEffect(() => {
    const supabase = createClient();

    supabase.auth.getUser().then(({ data: { user } }) => {
      setLabel(displayLabel(user));
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      setLabel(displayLabel(session?.user));
    });

    return () => subscription.unsubscribe();
  }, []);

  async function handleLogout() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/login");
    router.refresh();
  }

  if (label) {
    return (
      <div className="authStatusRow">
        <span className="userLabel">🔑 {label}</span>
        <button className="logoutBtn" onClick={handleLogout}>
          로그아웃
        </button>
      </div>
    );
  }

  return (
    <div className="authStatusRow">
      <Link href="/login" className="loginLink">
        🔐 보안 로그인
      </Link>
    </div>
  );
}
