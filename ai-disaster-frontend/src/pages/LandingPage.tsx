import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthContext'
import { homePath } from '../lib/transitions'
import '../styles/LandingPage.css'

export default function LandingPage() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const canvasRef = useRef<HTMLCanvasElement | null>(null)

  const handleLoginClick = () => {
    if ('vibrate' in navigator) {
      try {
        navigator.vibrate([15, 30, 20])
      } catch {
        /* ignore */
      }
    }
    if (user) {
      navigate(homePath(user.role))
    } else {
      navigate('/login')
    }
  }

  // ─────────────────────────────────────────────────────────────
  // WebGL Interactive Radar Shader (unchanged — keep as-is)
  // ─────────────────────────────────────────────────────────────
  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    function syncSize() {
      if (!canvas) return
      const w = canvas.clientWidth || window.innerWidth || 1280
      const h = canvas.clientHeight || window.innerHeight || 720
      if (canvas.width !== w || canvas.height !== h) {
        canvas.width = w
        canvas.height = h
      }
    }

    syncSize()
    const resizeObserver =
      typeof ResizeObserver !== 'undefined'
        ? new ResizeObserver(syncSize)
        : null
    if (resizeObserver) resizeObserver.observe(canvas)

    const gl =
      canvas.getContext('webgl') ||
      (canvas.getContext('experimental-webgl') as WebGLRenderingContext | null)
    if (!gl) return

    const vs = `
      attribute vec2 a_position;
      varying vec2 v_texCoord;
      void main() {
        v_texCoord = a_position * 0.5 + 0.5;
        gl_Position = vec4(a_position, 0.0, 1.0);
      }
    `

    const fs = `
      precision highp float;
      uniform float u_time;
      uniform vec2 u_resolution;
      uniform vec2 u_mouse;

      vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
      vec2 mod289(vec2 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
      vec3 permute(vec3 x) { return mod289(((x*34.0)+1.0)*x); }

      float snoise(vec2 v) {
        const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                           -0.577350269189626, 0.024390243902439);
        vec2 i  = floor(v + dot(v, C.yy) );
        vec2 x0 = v -   i + dot(i, C.xx);
        vec2 i1;
        i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
        vec4 x12 = x0.xyxy + C.xxzz;
        x12.xy -= i1;
        i = mod289(i);
        vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
              + i.x + vec3(0.0, i1.x, 1.0 ));
        vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
        m = m*m ;
        m = m*m ;
        vec3 x = 2.0 * fract(p * C.www) - 1.0;
        vec3 h = abs(x) - 0.5;
        vec3 ox = floor(x + 0.5);
        vec3 a0 = x - ox;
        m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
        vec3 g;
        g.x  = a0.x  * x0.x  + h.x  * x0.y;
        g.yz = a0.yz * x12.xz + h.yz * x12.yw;
        return 130.0 * dot(m, g);
      }

      void main() {
          vec2 uv = gl_FragCoord.xy / u_resolution.xy;
          vec2 centered = (gl_FragCoord.xy - 0.5 * u_resolution.xy) / min(u_resolution.x, u_resolution.y);
          vec3 col = vec3(0.03, 0.05, 0.09);
          vec2 gridUV = fract(uv * vec2(24.0, 48.0));
          float grid = (step(0.97, gridUV.x) + step(0.97, gridUV.y)) * 0.08;
          col += vec3(0.0, 0.5, 0.7) * grid;
          vec2 radarCenter = vec2(0.0, 0.25);
          vec2 toRadar = centered - radarCenter;
          float dist = length(toRadar);
          float angle = atan(toRadar.y, toRadar.x);
          float sweepAngle = mod(u_time * 1.5, 6.2831853) - 3.14159265;
          float angleDiff = mod(angle - sweepAngle + 6.2831853, 6.2831853);
          float sweep = smoothstep(2.2, 0.0, angleDiff) * smoothstep(0.85, 0.05, dist) * 0.45;
          col += vec3(0.0, 0.95, 0.85) * sweep;
          float ring1 = abs(dist - 0.2);
          float ring2 = abs(dist - 0.42);
          float ring3 = abs(dist - 0.65);
          float rings = smoothstep(0.008, 0.0, ring1) + smoothstep(0.008, 0.0, ring2) + smoothstep(0.008, 0.0, ring3);
          col += vec3(0.0, 0.8, 1.0) * rings * 0.22;
          float n = snoise(vec2(uv.x * 2.5 + u_time * 0.08, uv.y * 3.0 - u_time * 0.05));
          float heat = smoothstep(0.0, 0.8, n) * (1.0 - uv.y * 0.9);
          vec3 alertHeat = mix(vec3(0.0, 0.4, 0.6), vec3(0.95, 0.3, 0.1), heat * 0.5);
          col += alertHeat * heat * 0.25;
          float vignette = smoothstep(1.3, 0.2, length(centered));
          col *= vignette;
          gl_FragColor = vec4(col, 1.0);
      }
    `

    function cs(type: number, src: string) {
      const s = gl.createShader(type)
      if (!s) return null
      gl.shaderSource(s, src)
      gl.compileShader(s)
      return s
    }

    const vShader = cs(gl.VERTEX_SHADER, vs)
    const fShader = cs(gl.FRAGMENT_SHADER, fs)
    if (!vShader || !fShader) return

    const prog = gl.createProgram()
    if (!prog) return
    gl.attachShader(prog, vShader)
    gl.attachShader(prog, fShader)
    gl.linkProgram(prog)
    gl.useProgram(prog)

    const buf = gl.createBuffer()
    gl.bindBuffer(gl.ARRAY_BUFFER, buf)
    gl.bufferData(
      gl.ARRAY_BUFFER,
      new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]),
      gl.STATIC_DRAW
    )

    const pos = gl.getAttribLocation(prog, 'a_position')
    gl.enableVertexAttribArray(pos)
    gl.vertexAttribPointer(pos, 2, gl.FLOAT, false, 0, 0)

    const uTime = gl.getUniformLocation(prog, 'u_time')
    const uRes = gl.getUniformLocation(prog, 'u_resolution')
    const uMouse = gl.getUniformLocation(prog, 'u_mouse')

    let mouse = { x: canvas.width / 2, y: canvas.height / 2 }
    const onMouseMove = (event: MouseEvent) => {
      const rect = canvas.getBoundingClientRect()
      if (rect.width && rect.height) {
        const nx = (event.clientX - rect.left) / rect.width
        const ny = 1.0 - (event.clientY - rect.top) / rect.height
        mouse = { x: nx * canvas.width, y: ny * canvas.height }
      }
    }
    window.addEventListener('mousemove', onMouseMove)

    let animId: number
    function render(t: number) {
      syncSize()
      gl.viewport(0, 0, canvas.width, canvas.height)
      if (uTime) gl.uniform1f(uTime, t * 0.001)
      if (uRes) gl.uniform2f(uRes, canvas.width, canvas.height)
      if (uMouse) gl.uniform2f(uMouse, mouse.x, mouse.y)
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4)
      animId = requestAnimationFrame(render)
    }
    animId = requestAnimationFrame(render)

    return () => {
      cancelAnimationFrame(animId)
      window.removeEventListener('mousemove', onMouseMove)
      if (resizeObserver) resizeObserver.disconnect()
    }
  }, [])

  return (
    <div className="landing-container min-h-screen selection:bg-[#00f2fe] selection:text-[#006a70]">
      {/* ── Background layers ─────────────────────────────────── */}
      <div className="shader-canvas-container">
        <canvas
          ref={canvasRef}
          style={{ display: 'block', width: '100%', height: '100%' }}
        />
      </div>
      <div className="landing-grid-lines" />

      {/* ── Main content ──────────────────────────────────────── */}
      <main className="relative z-10 w-full max-w-[1400px] mx-auto px-5 sm:px-8 lg:px-12 xl:px-16 py-6 sm:py-8 lg:py-10 flex flex-col gap-6 sm:gap-8 lg:gap-10">

        {/* ═══ 1. HEADER ═══════════════════════════════════════ */}
        <header className="flex items-center justify-between pb-5 border-b border-[#3a494b]/40">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 sm:w-11 sm:h-11 rounded-xl bg-[#00f2fe]/10 border border-[#00f2fe]/30 flex items-center justify-center text-[#00f2fe] shadow-[0_0_15px_rgba(0,242,254,0.25)] shrink-0">
              <span className="material-symbols-outlined text-[22px] sm:text-[26px]">
                crisis_alert
              </span>
            </div>
            <div className="flex flex-col leading-tight">
              <span className="font-headline font-bold text-sm sm:text-base lg:text-lg tracking-wider text-white">
                DISASTER OPS CORE
              </span>
              <span className="font-telemetry text-[9px] sm:text-[10px] text-[#b9cacb] uppercase tracking-widest mt-0.5">
                AI Crisis Response Intelligence Network
              </span>
            </div>
          </div>

          <div className="flex items-center gap-3 sm:gap-4">
            <div className="hidden sm:inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#181b26]/90 border border-[#3a494b]/50 shadow-sm">
              <span className="w-2 h-2 rounded-full bg-[#00f2fe] animate-pulse" />
              <span className="font-telemetry text-[10px] sm:text-xs text-[#e0fdff] tracking-wide font-medium">
                SYSTEM ONLINE
              </span>
            </div>
            <button
              onClick={handleLoginClick}
              className="px-4 sm:px-5 py-2 sm:py-2.5 rounded-xl bg-[#00f2fe]/15 hover:bg-[#00f2fe]/25 text-[#00f2fe] hover:text-white border border-[#00f2fe]/40 font-headline font-semibold text-[11px] sm:text-xs tracking-wider uppercase transition-all duration-200 cursor-pointer shadow-[0_0_15px_rgba(0,242,254,0.15)] active:scale-95"
            >
              {user ? 'Dashboard' : 'Sign In'}
            </button>
          </div>
        </header>

        {/* ═══ 2. LIVE ALERT TICKER ════════════════════════════ */}
        <div className="relative w-full rounded-2xl bg-[#93000a]/25 backdrop-blur-md overflow-hidden py-3 px-4 flex items-center gap-3.5 shadow-lg shadow-[#93000a]/15 border border-[#93000a]/40">
          <div className="shrink-0 flex items-center justify-center">
            <span className="relative flex h-3 w-3">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#ffb4ab] opacity-75" />
              <span className="relative inline-flex rounded-full h-3 w-3 bg-[#ffb4ab]" />
            </span>
          </div>

          <div className="relative flex-1 overflow-hidden whitespace-nowrap">
            <div className="animate-marquee font-telemetry text-xs uppercase tracking-wider text-[#ffb4ab]">
              {/* Set A */}
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">warning</span>
                USGS SEISMIC ALERT: M5.8 Cascadia Subduction Zone
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">cyclone</span>
                IMD Cyclone Tracker Active
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">crisis_alert</span>
                GDACS Orange Alert
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
              {/* Set B (exact duplicate for seamless -50% loop) */}
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">warning</span>
                USGS SEISMIC ALERT: M5.8 Cascadia Subduction Zone
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">cyclone</span>
                IMD Cyclone Tracker Active
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
              <span className="flex items-center gap-2 font-semibold shrink-0">
                <span className="material-symbols-outlined text-[15px]">crisis_alert</span>
                GDACS Orange Alert
              </span>
              <span className="text-[#ffb4ab]/40 mx-6 shrink-0">•</span>
            </div>
          </div>

          <div className="shrink-0 px-3 py-1 rounded-full bg-[#ffb4ab]/20 text-[#ffb4ab] font-telemetry text-[11px] font-bold tracking-widest uppercase border border-[#ffb4ab]/30">
            LIVE
          </div>
        </div>

        {/* ═══ 3. HERO (2-COL: CONTENT + HUD) ══════════════════ */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 lg:gap-8 items-stretch">

          {/* ── 3a. Left: Hero Content ─────────────────────── */}
          <section className="lg:col-span-7 flex flex-col justify-between gap-6 sm:gap-8">

            <div className="flex flex-col gap-5 sm:gap-6">
              {/* Status badge */}
              <div className="inline-flex self-start items-center gap-2.5 px-3.5 py-1.5 rounded-full bg-[#262a35]/80 backdrop-blur-md shadow-sm border border-[#3a494b]/60">
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-[#00f2fe] opacity-90" />
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-[#00f2fe]" />
                </span>
                <span className="font-telemetry text-[10px] sm:text-xs text-[#e0fdff] uppercase tracking-wider font-semibold">
                  CORE MONITORING
                </span>
                <span className="w-1.5 h-1.5 rounded-full bg-[#3a494b]" />
                <span className="font-telemetry text-[10px] text-[#ffb95f] bg-[#472a00]/70 px-2 py-0.5 rounded font-bold tracking-widest">
                  DEFCON 2
                </span>
              </div>

              {/* Headline */}
              <h1 className="font-headline text-3xl sm:text-4xl lg:text-5xl xl:text-6xl font-bold text-[#e0e2f1] tracking-tight leading-[1.1]">
                Next-Gen AI Emergency
                <span className="block mt-1 bg-gradient-to-r from-[#00f2fe] via-[#7bd0ff] to-[#6ff6ff] bg-clip-text text-transparent drop-shadow-[0_0_24px_rgba(0,242,254,0.45)]">
                  Prediction &amp; Response Portal
                </span>
              </h1>

              {/* Subtitle */}
              <p className="text-sm sm:text-base lg:text-lg text-[#b9cacb] leading-relaxed max-w-2xl">
                Unified tactical response platform integrating live GIS telemetry,
                USGS seismic monitoring, automated weather intelligence, and
                human-in-the-loop responder dispatch.
              </p>
            </div>

            {/* CTA row */}
            <div className="flex flex-col sm:flex-row gap-3 sm:gap-4 w-full">
              <button
                onClick={handleLoginClick}
                className="group relative flex-1 overflow-hidden rounded-2xl bg-gradient-to-r from-[#00f2fe] to-[#00a6e0] p-[1px] active:scale-[0.98] transition-transform duration-150 shadow-xl shadow-[#00f2fe]/20 cursor-pointer"
                type="button"
              >
                <div className="relative flex items-center justify-between w-full px-5 sm:px-6 py-3.5 sm:py-4 rounded-2xl bg-[#0a0e18]/90 backdrop-blur-xl group-hover:bg-[#0a0e18]/70 transition-colors">
                  <div className="flex items-center gap-3">
                    <span className="material-symbols-outlined text-[#00f2fe] text-[20px] sm:text-[22px] transition-transform group-hover:rotate-45">
                      radar
                    </span>
                    <span className="font-headline text-xs sm:text-sm lg:text-base uppercase text-[#e0fdff] font-bold tracking-wider">
                      {user ? 'Open Operational Portal' : 'Login to System Portal'}
                    </span>
                  </div>
                  <div className="hidden sm:flex items-center gap-1.5 text-[#00f2fe]">
                    <span className="font-telemetry text-[10px] font-semibold tracking-wider uppercase">
                      {user ? 'AUTH_OK' : 'ACCESS_LOGIN'}
                    </span>
                    <span className="material-symbols-outlined text-[18px] group-hover:translate-x-1.5 transition-transform">
                      arrow_forward
                    </span>
                  </div>
                </div>
              </button>

              <button
                onClick={() => {
                  document.getElementById('capabilities-section')?.scrollIntoView({ behavior: 'smooth' })
                }}
                className="group relative flex items-center justify-between px-5 sm:px-6 py-3.5 sm:py-4 rounded-2xl bg-[#262a35]/60 backdrop-blur-md hover:bg-[#262a35]/90 text-[#e0e2f1] transition-all active:scale-[0.99] border border-[#3a494b]/60 cursor-pointer shadow-md"
                type="button"
              >
                <div className="flex items-center gap-3">
                  <span className="material-symbols-outlined text-[#7bd0ff] text-[20px]">track_changes</span>
                  <span className="font-headline text-xs sm:text-sm lg:text-base font-semibold tracking-wide text-[#7bd0ff] whitespace-nowrap">
                    Explore Capabilities
                  </span>
                </div>
                <span className="hidden sm:inline font-telemetry text-[10px] text-[#b9cacb] group-hover:text-[#00f2fe] transition-colors ml-3">
                  [MOD_04]
                </span>
              </button>
            </div>
          </section>

          {/* ── 3b. Right: Mission HUD ─────────────────────── */}
          <section className="lg:col-span-5 flex flex-col rounded-3xl bg-[#262a35]/80 backdrop-blur-xl p-5 sm:p-6 lg:p-7 shadow-2xl border border-[#3a494b]/60 relative overflow-hidden gap-4 sm:gap-5">

            {/* HUD header */}
            <div className="flex items-center justify-between gap-2 pb-3 border-b border-[#3a494b]/40">
              <div className="flex items-center gap-2.5 min-w-0">
                <span className="flex h-2.5 w-2.5 rounded-full bg-[#ffb4ab] animate-pulse shrink-0" />
                <span className="font-telemetry text-[10px] sm:text-xs lg:text-sm font-semibold tracking-wider text-[#e0e2f1] uppercase truncate">
                  TARGET: SECTOR 7 — BAY COASTAL
                </span>
              </div>
              <span className="shrink-0 px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#93000a]/60 text-[#ffb4ab] font-telemetry text-[9px] sm:text-[11px] font-bold border border-[#93000a]">
                CRITICAL
              </span>
            </div>

            {/* Radar display */}
            <div className="relative w-full h-56 sm:h-64 lg:h-72 rounded-2xl bg-[#0a0e18]/90 overflow-hidden flex items-center justify-center border border-[#3a494b]/50 shadow-inner">

              {/* Grid rings */}
              <svg
                className="absolute inset-0 w-full h-full text-[#00f2fe]/20"
                xmlns="http://www.w3.org/2000/svg"
              >
                <circle cx="50%" cy="50%" fill="none" r="22%" stroke="currentColor" strokeDasharray="3 3" strokeWidth="1.2" />
                <circle cx="50%" cy="50%" fill="none" r="42%" stroke="currentColor" strokeWidth="1.2" />
                <circle cx="50%" cy="50%" fill="none" r="62%" stroke="currentColor" strokeDasharray="2 4" strokeWidth="1.2" />
                <line stroke="currentColor" strokeOpacity="0.35" strokeWidth="1.2" x1="50%" x2="50%" y1="5%" y2="95%" />
                <line stroke="currentColor" strokeOpacity="0.35" strokeWidth="1.2" x1="5%" x2="95%" y1="50%" y2="50%" />
              </svg>

              {/* Radar sweep — quarter-circle anchored at center */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none overflow-hidden">
                <div className="w-full h-full animate-radar origin-center">
                  <div className="absolute top-0 left-0 w-1/2 h-1/2 bg-gradient-to-br from-[#00f2fe]/35 via-[#00f2fe]/8 to-transparent rounded-tl-full" />
                </div>
              </div>

              {/* Ping blips */}
              <div className="absolute top-[38%] left-[45%] flex items-center justify-center">
                <span className="animate-ping absolute h-5 w-5 rounded-full bg-[#ffb4ab] opacity-75" />
                <span className="relative h-2.5 w-2.5 rounded-full bg-[#ffb4ab]" />
                <span className="absolute -top-3.5 left-3.5 font-telemetry text-[9px] sm:text-[10px] text-[#ffb4ab] font-bold whitespace-nowrap">
                  EPICENTER 5.8M
                </span>
              </div>
              <div className="absolute bottom-[30%] right-[32%] flex items-center justify-center">
                <span className="animate-ping absolute h-4 w-4 rounded-full bg-[#00a6e0] opacity-60" />
                <span className="relative h-2 w-2 rounded-full bg-[#7bd0ff]" />
                <span className="absolute -bottom-3.5 -right-2 font-telemetry text-[8px] sm:text-[9px] text-[#7bd0ff] whitespace-nowrap">
                  SQ-04 AIR
                </span>
              </div>
              <div className="absolute top-[25%] right-[22%] flex items-center justify-center">
                <span className="relative h-2 w-2 rounded-full bg-[#ffddb8]" />
                <span className="absolute -top-4 left-2.5 font-telemetry text-[8px] sm:text-[9px] text-[#fff5ee] whitespace-nowrap">
                  BUOY #884
                </span>
              </div>

              {/* Corner telemetry */}
              <div className="absolute bottom-2.5 left-3 font-telemetry text-[9px] sm:text-[11px] text-[#b9cacb]/80 uppercase">
                COORDS: 45°21'N 124°10'W
              </div>
              <div className="absolute top-2.5 right-3 font-telemetry text-[9px] sm:text-[11px] text-[#00dce6]/90 font-bold">
                SCANNING 100%
              </div>
            </div>

            {/* HUD diagnostics */}
            <div className="flex flex-col gap-3">
              <div className="flex items-center justify-between gap-2">
                <span className="font-telemetry text-[10px] sm:text-xs lg:text-sm text-[#b9cacb] uppercase">
                  RISK COEFFICIENT
                </span>
                <span className="font-telemetry text-[10px] sm:text-xs lg:text-sm font-bold text-[#ffb4ab] bg-[#93000a]/30 px-2.5 py-0.5 sm:px-3 sm:py-1 rounded-lg border border-[#93000a]/50">
                  0.87 [HIGH]
                </span>
              </div>

              <div className="p-3 sm:p-3.5 rounded-2xl bg-[#0a0e18]/80 flex items-start gap-3 border border-[#3a494b]/40">
                <span className="material-symbols-outlined text-[#00f2fe] text-[20px] sm:text-[22px] mt-0.5 shrink-0">
                  outgoing_mail
                </span>
                <div className="flex flex-col gap-0.5 min-w-0">
                  <span className="font-telemetry text-[10px] sm:text-[11px] text-[#00f2fe] tracking-wide uppercase font-semibold">
                    AUTO-DISPATCH PIPELINE ARMED
                  </span>
                  <span className="text-[11px] sm:text-xs lg:text-sm text-[#e0e2f1] truncate">
                    SQUAD-04 (Air Rescue) &amp; SQUAD-09 (GIS Hazmat)
                  </span>
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* ═══ 4. TELEMETRY STRIP (FULL WIDTH) ══════════════════ */}
        <section
          aria-label="Live telemetry metrics"
          className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4 w-full p-3 sm:p-4 rounded-2xl bg-[#181b26]/75 backdrop-blur-md shadow-inner border border-[#3a494b]/40"
        >
          {/* Metric 1 */}
          <div className="flex flex-col p-3 sm:p-4 rounded-xl bg-[#0a0e18]/85 border border-[#3a494b]/30">
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase">Sensors</span>
              <span className="w-2 h-2 rounded-full bg-[#00f2fe]" />
            </div>
            <span className="font-telemetry text-lg sm:text-xl lg:text-2xl text-[#e0fdff] font-bold leading-tight">4,821</span>
            <span className="font-telemetry text-[9px] sm:text-[10px] text-[#00dce6]/90 mt-1">99.98% Up</span>
          </div>

          {/* Metric 2 */}
          <div className="flex flex-col p-3 sm:p-4 rounded-xl bg-[#0a0e18]/85 border border-[#3a494b]/30">
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase">Latency</span>
              <span className="w-2 h-2 rounded-full bg-[#7bd0ff]" />
            </div>
            <span className="font-telemetry text-lg sm:text-xl lg:text-2xl text-[#7bd0ff] font-bold leading-tight">14ms</span>
            <span className="font-telemetry text-[9px] sm:text-[10px] text-[#b9cacb] mt-1">AI Hazard</span>
          </div>

          {/* Metric 3 */}
          <div className="flex flex-col p-3 sm:p-4 rounded-xl bg-[#0a0e18]/85 border border-[#3a494b]/30">
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase">Squads</span>
              <span className="w-2 h-2 rounded-full bg-[#ffb95f]" />
            </div>
            <span className="font-telemetry text-lg sm:text-xl lg:text-2xl text-[#fff5ee] font-bold leading-tight">142</span>
            <span className="font-telemetry text-[9px] sm:text-[10px] text-[#ffb95f]/90 mt-1">Ready Standby</span>
          </div>

          {/* Metric 4 */}
          <div className="flex flex-col p-3 sm:p-4 rounded-xl bg-[#0a0e18]/85 border border-[#3a494b]/30">
            <div className="flex items-center justify-between mb-1.5">
              <span className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase">Mesh Nodes</span>
              <span className="w-2 h-2 rounded-full bg-[#00f2fe]" />
            </div>
            <span className="font-telemetry text-lg sm:text-xl lg:text-2xl text-[#e0fdff] font-bold leading-tight">128</span>
            <span className="font-telemetry text-[9px] sm:text-[10px] text-[#00dce6]/90 mt-1">Online</span>
          </div>
        </section>

        {/* ═══ 5. TACTICAL FEATURE GRID ═════════════════════════ */}
        <section id="capabilities-section" className="flex flex-col w-full gap-5">
          <div className="flex items-center justify-between px-1">
            <div className="flex items-center gap-3">
              <span className="material-symbols-outlined text-[#00f2fe] text-[22px] sm:text-[24px]">
                view_module
              </span>
              <h2 className="font-headline text-base sm:text-lg lg:text-xl uppercase text-[#e0e2f1] tracking-wider font-bold">
                Tactical Intelligence Capabilities
              </h2>
            </div>
            <span className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] whitespace-nowrap">
              [4 NODES ACTIVE]
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5 lg:gap-6">
            {/* Card 1 */}
            <div className="landing-interactive-card relative p-5 lg:p-6 rounded-2xl bg-[#1c1f2a]/75 backdrop-blur-md border border-[#3a494b]/50 shadow-md flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="w-11 h-11 lg:w-12 lg:h-12 rounded-xl bg-[#262a35] flex items-center justify-center text-[#00f2fe] shadow-inner border border-[#3a494b]/70">
                    <span className="material-symbols-outlined text-[22px] lg:text-[24px]">settings_input_antenna</span>
                  </div>
                  <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded font-telemetry text-[9px] sm:text-[10px] bg-[#00f2fe]/10 text-[#00f2fe] tracking-wider uppercase font-semibold border border-[#00f2fe]/30">
                    Live Ingest
                  </span>
                </div>
                <h3 className="font-headline text-base lg:text-lg text-[#e0e2f1] font-bold mb-2">
                  Live Sensor Ingestion
                </h3>
                <p className="text-xs sm:text-sm text-[#b9cacb] mb-4 lg:mb-5 leading-relaxed">
                  Real-time streaming ingestion pipeline connected with Open-Meteo,
                  USGS Seismology, GDACS, and IMD early-warning radar arrays.
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5 sm:gap-2 pt-3 border-t border-[#3a494b]/30">
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#b9cacb] border border-[#3a494b]/40">USGS GeoJSON</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#b9cacb] border border-[#3a494b]/40">GDACS Feed</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#b9cacb] border border-[#3a494b]/40">IMD Cyclone</span>
              </div>
            </div>

            {/* Card 2 */}
            <div className="landing-interactive-card relative p-5 lg:p-6 rounded-2xl bg-[#1c1f2a]/75 backdrop-blur-md border border-[#3a494b]/50 shadow-md flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="w-11 h-11 lg:w-12 lg:h-12 rounded-xl bg-[#262a35] flex items-center justify-center text-[#7bd0ff] shadow-inner border border-[#3a494b]/70">
                    <span className="material-symbols-outlined text-[22px] lg:text-[24px]">neurology</span>
                  </div>
                  <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded font-telemetry text-[9px] sm:text-[10px] bg-[#00a6e0]/20 text-[#7bd0ff] tracking-wider uppercase font-semibold border border-[#7bd0ff]/30">
                    Inference Core
                  </span>
                </div>
                <h3 className="font-headline text-base lg:text-lg text-[#e0e2f1] font-bold mb-2">
                  AI Hazard Prediction
                </h3>
                <p className="text-xs sm:text-sm text-[#b9cacb] mb-4 lg:mb-5 leading-relaxed">
                  XGBoost and neural spatial regression engines estimating terrain
                  vulnerability, flood inundation trajectories, and demographic risk zones.
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5 sm:gap-2 pt-3 border-t border-[#3a494b]/30">
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#7bd0ff] border border-[#3a494b]/40">Spatial ML</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#7bd0ff] border border-[#3a494b]/40">Flood Contours</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#7bd0ff] border border-[#3a494b]/40">Pop Density</span>
              </div>
            </div>

            {/* Card 3 */}
            <div className="landing-interactive-card relative p-5 lg:p-6 rounded-2xl bg-[#1c1f2a]/75 backdrop-blur-md border border-[#3a494b]/50 shadow-md flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="w-11 h-11 lg:w-12 lg:h-12 rounded-xl bg-[#262a35] flex items-center justify-center text-[#00dce6] shadow-inner border border-[#3a494b]/70">
                    <span className="material-symbols-outlined text-[22px] lg:text-[24px]">shield_with_heart</span>
                  </div>
                  <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded font-telemetry text-[9px] sm:text-[10px] bg-[#00dce6]/20 text-[#00dce6] tracking-wider uppercase font-semibold border border-[#00dce6]/30">
                    HITL Protocol
                  </span>
                </div>
                <h3 className="font-headline text-base lg:text-lg text-[#e0e2f1] font-bold mb-2">
                  Tactical Squad Dispatch
                </h3>
                <p className="text-xs sm:text-sm text-[#b9cacb] mb-4 lg:mb-5 leading-relaxed">
                  Automated squad allocation with human-in-the-loop validation,
                  optimal routing via offline mesh networks, and payload triage.
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5 sm:gap-2 pt-3 border-t border-[#3a494b]/30">
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#b9cacb] border border-[#3a494b]/40">Matchmaking</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#b9cacb] border border-[#3a494b]/40">Air/Ground Units</span>
              </div>
            </div>

            {/* Card 4 */}
            <div className="landing-interactive-card relative p-5 lg:p-6 rounded-2xl bg-[#1c1f2a]/75 backdrop-blur-md border border-[#3a494b]/50 shadow-md flex flex-col justify-between">
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="w-11 h-11 lg:w-12 lg:h-12 rounded-xl bg-[#262a35] flex items-center justify-center text-[#ffb95f] shadow-inner border border-[#3a494b]/70">
                    <span className="material-symbols-outlined text-[22px] lg:text-[24px]">map</span>
                  </div>
                  <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded font-telemetry text-[9px] sm:text-[10px] bg-[#ffd3a1]/20 text-[#ffb95f] tracking-wider uppercase font-semibold border border-[#ffb95f]/30">
                    SRID 4326
                  </span>
                </div>
                <h3 className="font-headline text-base lg:text-lg text-[#e0e2f1] font-bold mb-2">
                  Geospatial Mapping
                </h3>
                <p className="text-xs sm:text-sm text-[#b9cacb] mb-4 lg:mb-5 leading-relaxed">
                  High-cadence GIS polygon rendering powered by PostGIS raster
                  pipelines, live satellite thermal overlays, and topographical slices.
                </p>
              </div>
              <div className="flex flex-wrap gap-1.5 sm:gap-2 pt-3 border-t border-[#3a494b]/30">
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#ffb95f] border border-[#3a494b]/40">PostGIS Spatial</span>
                <span className="px-2 py-0.5 sm:px-2.5 sm:py-1 rounded bg-[#0a0e18] font-telemetry text-[10px] sm:text-[11px] text-[#ffb95f] border border-[#3a494b]/40">Topo Layers</span>
              </div>
            </div>
          </div>
        </section>

        {/* ═══ 6. SYS CONSOLE / DAEMON LOGS ═════════════════════ */}
        <section className="flex flex-col w-full rounded-2xl bg-[#0a0e18]/95 backdrop-blur-md p-5 sm:p-6 lg:p-7 shadow-xl border border-[#3a494b]/60 gap-4">
          <div className="flex items-center justify-between pb-3 border-b border-[#3a494b]/40">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full bg-[#ffb4ab]/80" />
              <span className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full bg-[#ffb95f]/80" />
              <span className="w-2.5 h-2.5 sm:w-3 sm:h-3 rounded-full bg-[#00f2fe]/80" />
              <span className="ml-2 font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase font-semibold">
                SYS_CONSOLE // DAEMON_LOGS
              </span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-[#00f2fe] animate-ping" />
              <span className="font-telemetry text-[10px] sm:text-xs text-[#00f2fe] font-bold">
                DAEMON_ACTIVE
              </span>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 font-telemetry text-[11px] sm:text-xs leading-relaxed text-[#b9cacb]">
            <div className="flex items-start gap-2.5 p-2.5 sm:p-3 rounded-xl bg-[#181b26]/60 border border-[#3a494b]/30">
              <span className="text-[#00f2fe] font-semibold shrink-0">[09:42:01]</span>
              <span className="text-[#e0e2f1] flex-1">[SYS_INIT] Ingesting USGS GeoJSON stream...</span>
              <span className="text-[#00dce6] font-bold shrink-0">[OK]</span>
            </div>
            <div className="flex items-start gap-2.5 p-2.5 sm:p-3 rounded-xl bg-[#181b26]/60 border border-[#3a494b]/30">
              <span className="text-[#00f2fe] font-semibold shrink-0">[09:42:04]</span>
              <span className="text-[#e0e2f1] flex-1">[ML_CORE] XGBoost flood model converged</span>
              <span className="text-[#7bd0ff] font-bold shrink-0">[98.4%]</span>
            </div>
            <div className="flex items-start gap-2.5 p-2.5 sm:p-3 rounded-xl bg-[#181b26]/60 border border-[#3a494b]/30">
              <span className="text-[#00f2fe] font-semibold shrink-0">[09:42:07]</span>
              <span className="text-[#e0e2f1] flex-1">[COMMS] Encrypted mesh network online</span>
              <span className="text-[#ffb95f] font-bold shrink-0">[SYNC]</span>
            </div>
            <div className="flex items-start gap-2.5 p-2.5 sm:p-3 rounded-xl bg-[#181b26]/60 border border-[#3a494b]/30">
              <span className="text-[#00f2fe] font-semibold shrink-0">[09:42:11]</span>
              <span className="text-[#ffb4ab] font-medium flex-1">[ALRT] Subduction slip anomaly in Zone 7.</span>
              <span className="text-[#ffb4ab] font-bold shrink-0">[BROADCAST]</span>
            </div>
          </div>
        </section>

        {/* ═══ 7. FOOTER ════════════════════════════════════════ */}
        <footer className="flex flex-col sm:flex-row items-center justify-between text-center sm:text-left pt-5 pb-2 gap-3 sm:gap-4 border-t border-[#3a494b]/40">
          <div className="flex items-center gap-2.5">
            <span className="material-symbols-outlined text-[#00f2fe] text-[18px] sm:text-[20px]">
              verified_user
            </span>
            <span className="font-telemetry text-[10px] sm:text-xs text-[#00f2fe] font-semibold tracking-wider uppercase">
              Secure Encrypted Bearer Token Auth
            </span>
          </div>

          <p className="font-telemetry text-[10px] sm:text-xs text-[#b9cacb] uppercase tracking-widest m-0">
            Disaster Emergency Response System &copy; 2026
          </p>

          <div className="flex items-center gap-2 sm:gap-3 font-telemetry text-[9px] sm:text-[10px] text-[#b9cacb]/80">
            <span>VERSION 2.4-PROD</span>
            <span className="text-[#3a494b]">•</span>
            <span>SRID 4326 GIS ENGINE</span>
          </div>
        </footer>
      </main>
    </div>
  )
}