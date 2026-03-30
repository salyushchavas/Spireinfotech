"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import {
  BookOpen,
  ArrowRight,
  ShieldCheck,
  GraduationCap,
  PlusCircle,
  Users,
  BarChart3,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { getEnrollments, requestInstructor } from "@/lib/api";
import { GlassCard } from "@/components/ui/GlassCard";
import { Button } from "@/components/ui/Button";

const fadeUp = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.4 },
};

const ROLE_CONFIG: Record<string, { label: string; color: string }> = {
  STUDENT: { label: "Student", color: "bg-emerald-100 text-emerald-700" },
  INSTRUCTOR: { label: "Instructor", color: "bg-violet-100 text-violet-700" },
  ADMIN: { label: "Admin", color: "bg-amber-100 text-amber-700" },
};

export default function DashboardPage() {
  const { user, isLoading: authLoading } = useAuth();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const [enrollments, setEnrollments] = useState<any[]>([]);
  const [enrollLoading, setEnrollLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [requestStatus, setRequestStatus] = useState<"idle" | "loading" | "success" | "error">("idle");
  const [requestMsg, setRequestMsg] = useState("");

  useEffect(() => {
    if (!user) return;
    setEnrollLoading(true);
    getEnrollments()
      .then((data) => setEnrollments(data ?? []))
      .catch(() => setEnrollments([]))
      .finally(() => setEnrollLoading(false));
  }, [user]);

  const handleRequestInstructor = async () => {
    setRequestStatus("loading");
    try {
      await requestInstructor();
      setRequestStatus("success");
      setRequestMsg("Request submitted! An admin will review it shortly.");
    } catch (err: unknown) {
      setRequestStatus("error");
      setRequestMsg(err instanceof Error ? err.message : "Failed to submit request.");
    }
  };

  // ─── Loading state ──────────────────────────────────────────────
  if (authLoading) {
    return (
      <section className="mx-auto max-w-7xl px-6 pt-32 pb-20 flex items-center justify-center min-h-[60vh]">
        <Loader2 size={32} className="animate-spin text-[#52B788]" />
      </section>
    );
  }

  // ─── Not logged in (shouldn't happen if middleware guards, but safe) ──
  if (!user) {
    return (
      <section className="mx-auto max-w-7xl px-6 pt-32 pb-20 text-center">
        <p className="text-gray-500">Please log in to view your dashboard.</p>
        <Button asChild className="mt-4">
          <Link href="/login">Go to Login</Link>
        </Button>
      </section>
    );
  }

  const role = user.role ?? "STUDENT";
  const roleInfo = ROLE_CONFIG[role] ?? ROLE_CONFIG.STUDENT;

  return (
    <section className="mx-auto max-w-7xl px-6 pt-32 pb-20">
      <motion.div {...fadeUp}>
        {/* ── Greeting + Role Badge ─────────────────────────────────── */}
        <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-8">
          <div>
            <h1 className="font-[family-name:var(--font-playfair)] text-3xl font-bold text-[#1B4332]">
              Welcome back, {user.fullName}!
            </h1>
            <p className="text-gray-500 mt-1">Here is your learning overview.</p>
          </div>
          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold ${roleInfo.color} w-fit`}>
            <ShieldCheck size={14} />
            {roleInfo.label}
          </span>
        </div>

        {/* ── Role-Based Actions ────────────────────────────────────── */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">
          {/* STUDENT: request instructor */}
          {role === "STUDENT" && (
            <GlassCard>
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
                  <GraduationCap size={20} className="text-emerald-600" />
                </div>
                <div>
                  <p className="font-semibold text-[#1a1a1a] text-sm">Become an Instructor</p>
                  <p className="text-xs text-gray-500">Share your knowledge with others</p>
                </div>
              </div>
              {requestStatus === "success" ? (
                <p className="text-xs text-emerald-600 font-medium">{requestMsg}</p>
              ) : requestStatus === "error" ? (
                <p className="text-xs text-red-500 font-medium">{requestMsg}</p>
              ) : (
                <Button
                  size="sm"
                  onClick={handleRequestInstructor}
                  disabled={requestStatus === "loading"}
                  className="w-full"
                >
                  {requestStatus === "loading" ? (
                    <Loader2 size={14} className="animate-spin mr-1" />
                  ) : null}
                  Request Instructor Status
                </Button>
              )}
            </GlassCard>
          )}

          {/* INSTRUCTOR: create course */}
          {role === "INSTRUCTOR" && (
            <GlassCard>
              <div className="flex items-center gap-3 mb-3">
                <div className="w-10 h-10 rounded-full bg-violet-100 flex items-center justify-center">
                  <PlusCircle size={20} className="text-violet-600" />
                </div>
                <div>
                  <p className="font-semibold text-[#1a1a1a] text-sm">Create a Course</p>
                  <p className="text-xs text-gray-500">Build and publish your curriculum</p>
                </div>
              </div>
              <Button size="sm" asChild className="w-full">
                <Link href="/courses/create">
                  Create Course <ArrowRight size={14} className="ml-1" />
                </Link>
              </Button>
            </GlassCard>
          )}

          {/* ADMIN: admin panel + stats */}
          {role === "ADMIN" && (
            <>
              <GlassCard>
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-amber-100 flex items-center justify-center">
                    <Users size={20} className="text-amber-600" />
                  </div>
                  <div>
                    <p className="font-semibold text-[#1a1a1a] text-sm">Admin Panel</p>
                    <p className="text-xs text-gray-500">Manage users and requests</p>
                  </div>
                </div>
                <Button size="sm" asChild className="w-full">
                  <Link href="/admin">
                    Open Admin Panel <ArrowRight size={14} className="ml-1" />
                  </Link>
                </Button>
              </GlassCard>
              <GlassCard>
                <div className="flex items-center gap-3 mb-3">
                  <div className="w-10 h-10 rounded-full bg-emerald-100 flex items-center justify-center">
                    <BarChart3 size={20} className="text-emerald-600" />
                  </div>
                  <div>
                    <p className="font-semibold text-[#1a1a1a] text-sm">Platform Analytics</p>
                    <p className="text-xs text-gray-500">View usage stats and metrics</p>
                  </div>
                </div>
                <Button size="sm" variant="secondary" asChild className="w-full">
                  <Link href="/admin/analytics">
                    View Analytics <ArrowRight size={14} className="ml-1" />
                  </Link>
                </Button>
              </GlassCard>
            </>
          )}
        </div>

        {/* ── Error Banner ──────────────────────────────────────────── */}
        {error && (
          <GlassCard className="mb-6 border-red-200 bg-red-50/80">
            <div className="flex items-center gap-2 text-red-600">
              <AlertCircle size={16} />
              <p className="text-sm">{error}</p>
            </div>
          </GlassCard>
        )}

        {/* ── Enrolled Courses ──────────────────────────────────────── */}
        <h2 className="text-xl font-bold text-[#1B4332] mb-4">Your Courses</h2>

        {enrollLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 size={24} className="animate-spin text-[#52B788]" />
          </div>
        ) : enrollments.length === 0 ? (
          <GlassCard className="text-center py-12 mb-10">
            <BookOpen size={40} className="mx-auto text-[#52B788]/40 mb-3" />
            <p className="text-gray-500 text-sm mb-4">You have not enrolled in any courses yet.</p>
            <Button size="sm" asChild>
              <Link href="/courses">
                Browse Courses <ArrowRight size={14} className="ml-1" />
              </Link>
            </Button>
          </GlassCard>
        ) : (
          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 mb-10">
            {enrollments.map((enrollment) => {
              const course = enrollment.course ?? enrollment;
              const id = course.id ?? enrollment.courseId;
              const title = course.title ?? "Untitled Course";
              return (
                <Link key={id} href={`/courses/${id}`}>
                  <GlassCard hover className="h-full">
                    <div className="h-28 -mx-6 -mt-6 mb-4 rounded-t-2xl bg-gradient-to-br from-[#1B4332]/10 to-[#2D6A4F]/20 flex items-center justify-center">
                      <BookOpen size={24} className="text-[#1B4332]/30" />
                    </div>
                    <h3 className="font-semibold text-sm text-[#1a1a1a] line-clamp-2">
                      {title}
                    </h3>
                    {course.shortDescription && (
                      <p className="text-xs text-gray-500 mt-1 line-clamp-2">
                        {course.shortDescription}
                      </p>
                    )}
                  </GlassCard>
                </Link>
              );
            })}
          </div>
        )}
      </motion.div>
    </section>
  );
}
