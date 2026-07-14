#version 150

in vec2 vertexCoordinate;
in vec2 vertexScale;

uniform sampler2D DiffuseSampler;

out vec4 fragColor;

void main() {
    float xWidth = fwidth(vertexCoordinate.x);
    float yWidth = fwidth(vertexCoordinate.y);

    float xDist = 0.5 - 0.5 * abs(vertexCoordinate.x);
    float yDist = 0.5 - 0.5 * abs(vertexCoordinate.y);

    float alpha = 1 - min((xDist - 0.012 * vertexScale.x) / xWidth, (yDist - 0.012 * vertexScale.y) / yWidth);
    fragColor = vec4(1.0, 0.0, 0.0, clamp(alpha, 0.15, 1.0));
}
