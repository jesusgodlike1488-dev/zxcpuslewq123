#version 150
in vec2 texCoord;
out vec4 fragColor;

uniform vec2 u_size;
uniform float u_radius;
uniform vec4 u_color;
uniform vec4 u_glow_color;
uniform float u_glow_radius;
uniform float u_border_thickness;

// Гениальная математика SDF
float roundedSDF(vec2 centerPos, vec2 size, float radius) {
    vec2 q = abs(centerPos) - size + radius;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - radius;
}

void main() {
    vec2 totalSize = u_size + vec2(u_glow_radius * 2.0);
    vec2 pixelPos = texCoord * totalSize;
    vec2 center = totalSize / 2.0;

    // Дистанция от пикселя до края формы
    float dist = roundedSDF(pixelPos - center, u_size / 2.0, u_radius);

    float smoothedAlpha = 0.0;

    if (u_border_thickness > 0.0) {
        // Режим обводки (Outline)
        float outlineDist = abs(dist + u_border_thickness / 2.0) - u_border_thickness / 2.0;
        smoothedAlpha = 1.0 - smoothstep(0.0, 1.0, outlineDist);
    } else {
        // Режим заливки (Fill)
        smoothedAlpha = 1.0 - smoothstep(0.0, 1.0, dist);
    }

    vec4 boxColor = vec4(u_color.rgb, u_color.a * smoothedAlpha);

    // Режим свечения (Glow)
    float glowAlpha = 0.0;
    if (dist > 0.0 && u_glow_radius > 0.0) {
        glowAlpha = exp(-dist / (u_glow_radius * 0.4)) * u_glow_color.a;
    }
    vec4 glow = vec4(u_glow_color.rgb, glowAlpha);

    fragColor = mix(glow, boxColor, smoothedAlpha);
}