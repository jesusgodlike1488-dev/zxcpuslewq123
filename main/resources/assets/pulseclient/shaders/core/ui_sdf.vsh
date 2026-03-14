#version 150 core

// ═══════════════════════════════════════════════════════════════════
//  PulseClient — SDF UI Vertex Shader
//  Принимает Position + UV0 от Tessellator (VertexFormats.POSITION_TEXTURE)
//  и передаёт во фрагментный шейдер нормализованные UV в диапазоне [0,1]
// ═══════════════════════════════════════════════════════════════════

in vec3 Position;
in vec2 UV0;

// Стандартная матрица проекции Minecraft (ModelViewMat не нужна —
// мы рисуем в screen-space, MatrixStack уже заложена в вершины)
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// varying для фрагментного шейдера — UV квада [0..1]
out vec2 v_LocalUV;

void main() {
    // ⚠️ MC 1.20.4: Tessellator передаёт Position уже в screen-space,
    // но GameRenderer требует умножение на ProjMat * ModelViewMat
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    // UV0 заполняется вручную при построении квада: (0,0) → (1,1)
    v_LocalUV = UV0;
}
