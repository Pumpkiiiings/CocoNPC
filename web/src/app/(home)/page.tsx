"use client";

import React, { useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";

/** Support server. Also linked from the docs sidebar and README. */
const SUPPORT_DISCORD = "https://discord.gg/ydsUw5UJrB";

export default function HomePage() {
  const [copiedCmd, setCopiedCmd] = useState<string | null>(null);

  const copyText = (key: string, text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCmd(key);
    setTimeout(() => setCopiedCmd(null), 3000);
  };

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.8 }}
      style={{ display: "flex", flexDirection: "column", width: "100%", overflow: "hidden" }}
    >
      {/* 1. HERO SECTION */}
      <section style={{
        position: "relative",
        width: "100%",
        minHeight: "85vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        padding: "80px 20px 40px",
      }}>
        <motion.div 
          style={{ 
            position: "relative", 
            zIndex: 1, 
            display: "flex", 
            flexDirection: "column", 
            alignItems: "center", 
            gap: "28px",
            maxWidth: "900px",
            textAlign: "center"
          }}
        >
          {/* Info Badge with Static Warm Glow */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: "easeOut", delay: 0.2 }}
            className="info-badge"
          >
            <div className="static-glow" />
            <span style={{ position: "relative", zIndex: 1, fontSize: "0.88rem", fontWeight: "700", letterSpacing: "1px", color: "#f3f4f6" }}>
              ✨ COCONPC V1.0.0 • NEXT-GEN DISPLAY ENTITIES
            </span>
          </motion.div>

          {/* Static Amber Gold Title */}
          <motion.h1 
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: "easeOut", delay: 0.4 }}
            className="text-gradient"
            id="hero-title"
            style={{ 
              fontSize: "clamp(3.5rem, 8vw, 7.5rem)", 
              fontWeight: 900, 
              letterSpacing: "-0.05em",
              margin: 0,
              textShadow: "0 20px 60px rgba(0, 0, 0, 0.6)"
            }}
          >
            COCONPC
          </motion.h1>
          
          <motion.h2
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: "easeOut", delay: 0.6 }}
            style={{
              fontSize: "clamp(1.2rem, 2.5vw, 1.8rem)",
              color: "rgba(255, 255, 255, 0.75)",
              fontWeight: 400,
              margin: 0,
              lineHeight: 1.5,
              letterSpacing: "0.01em"
            }}
          >
            More than just a traditional NPC plugin.<br/>
            <span style={{ color: "#ffffff", fontWeight: 600 }}>A masterpiece of millimetric precision.</span>
          </motion.h2>

          {/* Action Buttons */}
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, ease: "easeOut", delay: 0.8 }}
            style={{ display: "flex", gap: "20px", marginTop: "15px", flexWrap: "wrap", justifyContent: "center" }}
          >
            <Link href="/docs" className="btn-primary" id="btn-docs">
              📖 Explore Documentation
            </Link>
            <button
              onClick={() => document.getElementById("start")?.scrollIntoView({ behavior: "smooth" })}
              className="btn-glass"
              id="btn-start"
            >
              ⚡ Commands & Setup
            </button>
            <a
              href={SUPPORT_DISCORD}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-discord"
              id="btn-support"
            >
              💬 Support & Discord
            </a>
          </motion.div>
        </motion.div>
      </section>

      {/* MAIN CONTENT */}
      <main style={{ maxWidth: "1100px", margin: "0 auto", padding: "40px 20px 100px", width: "100%", display: "flex", flexDirection: "column", gap: "110px" }}>
        
        {/* 2. WHY COCONPC */}
        <motion.section 
          id="why-coconpc"
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.8, ease: "easeOut" }}
        >
          <h2 style={{ fontSize: "2.5rem", fontWeight: "bold", textAlign: "center", marginBottom: "50px", color: "white", letterSpacing: "-0.02em" }}>
            WHY CHOOSE <span className="text-gradient">COCONPC</span>?
          </h2>

          <div style={{ display: "flex", flexDirection: "column", gap: "16px", maxWidth: "850px", margin: "0 auto" }}>
            {[
              { icon: "🗿", title: "Zero Lag & No Heavy Entities", desc: "Exclusively powered by Minecraft Display Entities with zero artificial intelligence computation, guaranteeing 0 TPS impact and flawless server performance." },
              { icon: "✨", title: "Real-Time Hologram Editor", desc: "Customize text shadowing, background color opacity, and dynamic billboard orientations on the fly via responsive in-game GUI menus without abrupt closures." },
              { icon: "🌗", title: "Adaptive Day/Night Shading", desc: "Zero forced artificial brightness. Your NPCs dynamically adapt to surrounding torchlight and solar cycles while projecting authentic ground shadows." },
              { icon: "⚡", title: "Persistent Action Chains", desc: "Assign clickable multi-command executions (chat, console) and interactive dialogues that reliably survive server reboots without any data loss." },
              { icon: "🎨", title: "100% Customizable Menus & Fillers", desc: "Clean item lore without ghost italics, fully editable GUI filler slots, and an intuitive modern YAML architecture." }
            ].map((item, idx) => (
              <div key={idx} style={{ 
                display: "flex", 
                gap: "22px", 
                alignItems: "center",
                background: "rgba(255, 255, 255, 0.025)",
                border: "1px solid rgba(255, 255, 255, 0.07)",
                padding: "22px 32px",
                borderRadius: "18px",
                backdropFilter: "blur(18px)",
                transition: "all 0.28s ease",
                cursor: "default"
              }}
              onMouseOver={e => {
                e.currentTarget.style.background = "rgba(255, 255, 255, 0.05)";
                e.currentTarget.style.borderColor = "rgba(245, 158, 11, 0.35)";
                e.currentTarget.style.transform = "translateX(6px)";
              }}
              onMouseOut={e => {
                e.currentTarget.style.background = "rgba(255, 255, 255, 0.025)";
                e.currentTarget.style.borderColor = "rgba(255, 255, 255, 0.07)";
                e.currentTarget.style.transform = "translateX(0px)";
              }}
              >
                <div style={{ fontSize: "2.2rem", flexShrink: 0 }}>{item.icon}</div>
                <div>
                  <h4 style={{ margin: "0 0 6px 0", color: "#ffffff", fontSize: "1.2rem", fontWeight: "700" }}>{item.title}</h4>
                  <p style={{ margin: 0, color: "rgba(255, 255, 255, 0.68)", fontSize: "0.98rem", lineHeight: "1.5" }}>{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </motion.section>

        {/* 3. QUICK START COMMANDS */}
        <motion.section 
          id="start" 
          initial={{ opacity: 0, y: 50 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          style={{ display: "flex", flexDirection: "column", alignItems: "center" }}
        >
          <div style={{ marginBottom: "50px", textAlign: "center" }}>
            <h2 style={{ fontSize: "2.5rem", color: "white", fontWeight: "bold", margin: 0, letterSpacing: "-0.02em" }}>
              HOW TO GET STARTED
            </h2>
            <p style={{ color: "rgba(255, 255, 255, 0.65)", marginTop: "12px", fontSize: "1.08rem" }}>
              Click on any card below to instantly copy the command to your clipboard.
            </p>
          </div>
          
          <div style={{ display: "flex", flexDirection: "column", gap: "22px", width: "100%", maxWidth: "900px" }}>
            
            {/* Create Command Card */}
            <div 
              className="glass-card" 
              onClick={() => copyText("create", "/coconpc create 1 Villager")}
              style={{ padding: "28px 34px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "20px", cursor: "pointer", flexWrap: "wrap" }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
                <div style={{ width: "64px", height: "64px", background: "rgba(255, 255, 255, 0.07)", borderRadius: "16px", display: "flex", alignItems: "center", justifyContent: "center", border: "1px solid rgba(255,255,255,0.08)" }}>
                  <span style={{ fontSize: "2.2rem" }}>🛠️</span>
                </div>
                <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <h3 style={{ fontSize: "1.45rem", fontWeight: "bold", margin: 0, color: "white" }}>Create New NPC</h3>
                  <span style={{ color: "rgba(255, 255, 255, 0.6)", fontSize: "0.95rem" }}>Spawn a custom display entity with automated hitbox and identifier</span>
                </div>
              </div>
              <div style={{ 
                background: copiedCmd === "create" ? "rgba(245, 158, 11, 0.18)" : "rgba(255, 255, 255, 0.05)",
                border: copiedCmd === "create" ? "1px solid rgba(245, 158, 11, 0.65)" : "1px solid rgba(255, 255, 255, 0.12)",
                padding: "14px 28px", borderRadius: "14px", transition: "all 0.3s ease" 
              }}>
                <strong style={{ fontSize: "1.08rem", letterSpacing: "0.5px", color: copiedCmd === "create" ? "#fbbf24" : "white", fontFamily: "monospace" }}>
                  {copiedCmd === "create" ? "✓ COPIED!" : "/coconpc create 1 Villager"}
                </strong>
              </div>
            </div>

            {/* Edit Command Card */}
            <div 
              className="glass-card" 
              onClick={() => copyText("edit", "/coconpc edit 1")}
              style={{ padding: "28px 34px", display: "flex", alignItems: "center", justifyContent: "space-between", gap: "20px", cursor: "pointer", flexWrap: "wrap" }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
                <div style={{ width: "64px", height: "64px", background: "rgba(255, 255, 255, 0.07)", borderRadius: "16px", display: "flex", alignItems: "center", justifyContent: "center", border: "1px solid rgba(255,255,255,0.08)" }}>
                  <span style={{ fontSize: "2.2rem" }}>✨</span>
                </div>
                <div style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
                  <h3 style={{ fontSize: "1.45rem", fontWeight: "bold", margin: 0, color: "white" }}>Hologram & NPC Editor</h3>
                  <span style={{ color: "rgba(255, 255, 255, 0.6)", fontSize: "0.95rem" }}>Open the server GUI menu to adjust lines, shadows, and billboard rotation</span>
                </div>
              </div>
              <div style={{ 
                background: copiedCmd === "edit" ? "rgba(245, 158, 11, 0.18)" : "rgba(255, 255, 255, 0.05)",
                border: copiedCmd === "edit" ? "1px solid rgba(245, 158, 11, 0.65)" : "1px solid rgba(255, 255, 255, 0.12)",
                padding: "14px 28px", borderRadius: "14px", transition: "all 0.3s ease" 
              }}>
                <strong style={{ fontSize: "1.08rem", letterSpacing: "0.5px", color: copiedCmd === "edit" ? "#fbbf24" : "white", fontFamily: "monospace" }}>
                  {copiedCmd === "edit" ? "✓ COPIED!" : "/coconpc edit 1"}
                </strong>
              </div>
            </div>

          </div>
        </motion.section>

      </main>
    </motion.div>
  );
}
