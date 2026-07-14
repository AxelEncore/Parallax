#version 150

uniform sampler2D InSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

void main(){
    vec4 center = texture(InSampler, texCoord);

    if (center.a > 0.5) {
        discard;
    }

    vec4 left = texture(InSampler, texCoord - vec2(oneTexel.x * 2, 0.0));
    vec4 right = texture(InSampler, texCoord + vec2(oneTexel.x * 2, 0.0));
    vec4 up = texture(InSampler, texCoord - vec2(0.0, oneTexel.y * 2));
    vec4 down = texture(InSampler, texCoord + vec2(0.0, oneTexel.y * 2));

    float totalAlpha = left.a + right.a + up.a + down.a;
    if (totalAlpha > 0.5) {
        // fragColor = vec4(1.0, 0.0, 0.0, 1.0);
        fragColor = vec4((left.rgb*left.a + right.rgb*right.a + up.rgb*up.a + down.rgb*down.a) / totalAlpha, 1.0); // vec4(1.0, 0.9, 0.12, 1.0);
    } else {
        discard;
    }
}
