#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

layout (std140) uniform InstancedOffsetsBlock {
    vec4 InstancedOffsets[1000];
};

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord2;

void main() {
    vec4 offset = InstancedOffsets[gl_InstanceID];
    gl_Position = ProjMat * ModelViewMat * vec4(Position + offset.xyz, 1.0);

    vertexColor = Color;
    texCoord0 = UV0;
    texCoord2 = UV2;
}
