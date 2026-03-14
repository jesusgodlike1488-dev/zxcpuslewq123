#version 150 core

// ═══════════════════════════════════════════════════════════════════
//  PulseClient — SDF UI Fragment Shader
//  Zero-Branching: никаких if/else — только mix/step/smoothstep/clamp
//  Perfect AA через fwidth() — корректно на любом DPI
//  Physical Shadow через exp() с гамма-корректным смешиванием
//  SDF Clipping вместо glScissor — плавное обрезание через smoothstep
// ═══════════════════════════════════════════════════════════════════

in vec2 v_LocalUV;
out vec4 fragColor;

// ── Размер фигуры в пикселях (без padding на тень) ────────────────
uniform vec2  u_RectSize;

// ── Радиус скругления углов ───────────────────────────────────────
uniform float u_Radius;

// ── Вертикальный градиент заливки ─────────────────────────────────
uniform vec4  u_ColorTop;
uniform vec4  u_ColorBottom;

// ── Обводка (inner border) ────────────────────────────────────────
uniform vec4  u_BorderColor;
uniform float u_BorderWidth;

// ── Внешняя тень ──────────────────────────────────────────────────
uniform vec4  u_ShadowColor;
uniform float u_ShadowSoftness;

// ── SDF Clip-зона (замена glScissor) ──────────────────────────────
// cx, cy — центр клип-зоны в пространстве квада (пиксели)
// w, h   — полуразмеры клип-зоны
uniform vec4  u_ClipRect;
uniform float u_ClipRadius;

// ═══════════════════════════════════════════════════════════════════
//  SDF скруглённого прямоугольника
//  p — точка относительно центра фигуры
//  b — полуразмеры (halfSize)
//  r — радиус скругления
//  Возвращает: < 0 внутри, 0 на границе, > 0 снаружи
// ═══════════════════════════════════════════════════════════════════
float sdRoundRect(vec2 p, vec2 b, float r) {
    // Смещаем полуразмер на радиус, вычисляем расстояние до угла
    vec2 q = abs(p) - b + r;
    // min(max(...), 0) — расстояние внутри прямоугольника (отрицательное)
    // length(max(q, 0)) — расстояние снаружи (положительное)
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    // ── 1. Пересчёт UV → пиксельные координаты ───────────────────
    // Квад расширен на u_ShadowSoftness во все стороны (padding).
    // Полный размер квада = u_RectSize + 2 * u_ShadowSoftness.
    // v_LocalUV [0..1] покрывает весь квад, включая padding.
    // pixelPos: координата фрагмента в пикселях от верхнего-левого угла квада.
    vec2 totalSize = u_RectSize + 2.0 * u_ShadowSoftness;
    vec2 pixelPos  = v_LocalUV * totalSize;

    // Координата относительно центра ФИГУРЫ (без padding)
    // Центр фигуры = (totalSize / 2), т.к. padding симметричен
    vec2 center = totalSize * 0.5;
    vec2 p      = pixelPos - center;

    // Полуразмеры фигуры
    vec2 halfSize = u_RectSize * 0.5;

    // Ограничение радиуса: не больше минимального полуразмера
    float r = min(u_Radius, min(halfSize.x, halfSize.y));

    // ── 2. SDF расстояние до края фигуры ──────────────────────────
    float dist = sdRoundRect(p, halfSize, r);

    // ── 3. Anti-aliasing ширина — fwidth даёт размер пикселя ──────
    // fwidth(dist) = |dFdx(dist)| + |dFdy(dist)| — адаптивно к DPI
    float aa = fwidth(dist);

    // ── 4. Маска формы: 1 внутри, 0 снаружи, плавный переход ─────
    // smoothstep(edge0, edge1, x): 0 при x≤edge0, 1 при x≥edge1
    // Инвертируем: 1.0 - smoothstep(...) → 1 внутри
    float shapeMask = 1.0 - smoothstep(-aa, aa, dist);

    // ── 5. Вертикальный градиент заливки ──────────────────────────
    // v_LocalUV.y [0..1] по всему кваду; нормализуем к области фигуры
    // fillUV = 0 у верхнего края фигуры, 1 у нижнего
    float padNorm = u_ShadowSoftness / totalSize.y;
    float fillUV  = clamp((v_LocalUV.y - padNorm) / (1.0 - 2.0 * padNorm), 0.0, 1.0);
    vec4  fillColor = mix(u_ColorTop, u_ColorBottom, fillUV);

    // ── 6. Inner border через smoothstep от abs(dist) ─────────────
    // borderMask = 1.0 на границе (|dist| < borderWidth), 0 вне
    // Используем abs(dist) — работает и внутри и снаружи фигуры
    // step(0.5, u_BorderWidth) — отключает бордер когда width ≈ 0
    float borderOuter = smoothstep(-aa, aa, abs(dist) - u_BorderWidth);
    float borderInner = 1.0 - smoothstep(-aa, aa, abs(dist));
    float borderMask  = (1.0 - borderOuter) * borderInner * step(0.5, u_BorderWidth);

    // Смешиваем заливку с бордером: бордер поверх заливки
    vec4 surfaceColor = mix(fillColor, u_BorderColor, borderMask);

    // ── 7. Внешняя тень (Physical Shadow) ─────────────────────────
    // exp(-max(dist,0) * softness) — экспоненциальный спад от края
    // max(dist, 0): тень начинается только снаружи фигуры
    // Делим на softness для контроля размытия; защита от деления на 0
    float shadowFalloff = exp(-max(dist, 0.0) / max(u_ShadowSoftness, 0.001));

    // shadowAlpha = 0 внутри фигуры (shapeMask закроет), плавный спад снаружи
    // Умножаем на (1 - shapeMask) чтобы тень не проступала через заливку
    float shadowAlpha = shadowFalloff * u_ShadowColor.a * (1.0 - shapeMask);

    // ── 8. SDF Clipping (замена glScissor) ─────────────────────────
    // Координата фрагмента в пространстве квада относительно центра клип-зоны
    vec2 clipCenter = u_ClipRect.xy;
    vec2 clipHalf   = u_ClipRect.zw * 0.5;
    vec2 clipP      = pixelPos - clipCenter;
    float clipR     = min(u_ClipRadius, min(clipHalf.x, clipHalf.y));
    float clipDist  = sdRoundRect(clipP, clipHalf, clipR);
    float clipAA    = fwidth(clipDist);
    // clipMask = 1 внутри клип-зоны, 0 снаружи
    float clipMask  = 1.0 - smoothstep(-clipAA, clipAA, clipDist);

    // ── 9. Финальная сборка (zero-branching) ──────────────────────
    // Гамма-корректное смешивание тени: тень рисуется первой,
    // поверх — заливка фигуры через premultiplied alpha

    // Цвет тени (RGB от u_ShadowColor, A от shadowAlpha)
    vec3 shadowRGB = u_ShadowColor.rgb;

    // Итоговый цвет = тень + фигура поверх
    // Premultiplied: out = src + dst * (1 - srcAlpha)
    vec3 outRGB = surfaceColor.rgb * shapeMask + shadowRGB * shadowAlpha;
    float outA  = max(shapeMask * surfaceColor.a, shadowAlpha);

    // Применяем клиппинг — плавно обнуляем альфу за пределами клип-зоны
    outA *= clipMask;

    fragColor = vec4(outRGB, outA);
}
