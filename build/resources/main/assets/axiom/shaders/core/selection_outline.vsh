#version 150

in vec4 Position;

//#if MC>=12106
//$$ #moj_import <minecraft:projection.glsl>
//$$ layout(std140) uniform InOutSize {
//$$     vec2 InSize;
//$$     vec2 OutSize;
//$$ };
//#else
uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;
//#endif

out vec2 texCoord;
out vec2 oneTexel;

void main(){
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    oneTexel = 1.0 / InSize;
    texCoord = vec2(Position.x, OutSize.y - Position.y) / OutSize;
}
