#version 150

in vec4 vertexColor;
in vec2 vertexCoordinate;

uniform sampler2D DiffuseSampler;

out vec4 fragColor;

void main() {
    float x = mod(vertexCoordinate.x + 0.5, 1.0);
    float y = mod(vertexCoordinate.y + 0.5, 1.0);

    float xWidth = fwidth(vertexCoordinate.x);// * 0.75;
    float yWidth = fwidth(vertexCoordinate.y);// * 0.75;

    float xDist = abs(x-0.5);
    float yDist = abs(y-0.5);

//        float thresh = -0.1 + 0.025/min(xWidth, yWidth);
//    if (max(xDist, yDist) > 0.25) {
//        fragColor = vec4(vertexColor.rgb, 0.15);
//        return;
//    }

    // todo: depending on what we want, might be able to remove the alpha cap?
    // and then when we blit back, can reduce opacity there

    float alpha = 1 - min((xDist - 0.012) / xWidth, (yDist - 0.012) / yWidth);
    if (alpha > 1.0) alpha = 1.0;
    fragColor = vec4(vertexColor.rgb, max(0.15, alpha * vertexColor.a));
}
