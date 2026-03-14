#version 150
in vec2 Position;
in vec2 UV0;
uniform vec2 u_resolution;
out vec2 texCoord;

void main() {
    // Гениальная конвертация экранных пикселей в OpenGL координаты
    float x = (Position.x / u_resolution.x) * 2.0 - 1.0;
    float y = 1.0 - (Position.y / u_resolution.y) * 2.0;
    gl_Position = vec4(x, y, 0.0, 1.0);
    texCoord = UV0;
}