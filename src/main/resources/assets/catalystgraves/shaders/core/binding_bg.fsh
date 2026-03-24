#version 150

uniform float Time;
uniform vec4 ColorModulator;
uniform float IsValid;

uniform sampler2D Sampler0; 

in vec2 texCoord0;
out vec4 fragColor;

const mat2 mtx = mat2( vec2(0.80, -0.60), vec2(0.60, 0.80) );

float rand(vec2 n) { 
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

float noise(vec2 p){
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u * u * (3.0 - 2.0 * u);
    float res = mix(
        mix(rand(ip), rand(ip + vec2(1.0, 0.0)), u.x),
        mix(rand(ip + vec2(0.0, 1.0)), rand(ip + vec2(1.0, 1.0)), u.x), u.y);
    return res * res;
}

float fbm( vec2 p )
{
    float f = 0.0;
    // Eliminado el incremento de velocidad.
    float iTime = Time * 0.5; 
    
    f += 0.500000 * noise( p + iTime ); p = mtx * p * 2.02;
    f += 0.031250 * noise( p ); p = mtx * p * 2.01;
    f += 0.250000 * noise( p ); p = mtx * p * 2.03;
    f += 0.125000 * noise( p ); p = mtx * p * 2.01;
    f += 0.062500 * noise( p ); p = mtx * p * 2.04;
    f += 0.015625 * noise( p + sin(iTime) );

    return f / 0.96875;
}

float pattern( in vec2 p )
{
    return fbm( p + fbm( p + fbm( p ) ) );
}

vec4 colormap(float x) {
    vec4 good_low = vec4(0.01, 0.41, 0.51, 1.0);
    vec4 good_mid = vec4(0.50, 0.10, 0.30, 1.0);
    
    vec4 bad_low = vec4(0.20, 0.0, 0.0, 1.0);
    vec4 bad_mid = vec4(0.60, 0.10, 0.0, 1.0);
    
    vec4 color_low = mix(bad_low, good_low, IsValid);
    vec4 color_mid = mix(bad_mid, good_mid, IsValid);
    vec4 color_high = vec4(1.0, 1.0, 1.0, 1.0); 

    if (x < 0.24) return mix(color_low, color_mid, x / 0.24);
    else return mix(color_mid, color_high, (x - 0.24) / 0.76);
}

void main()
{
    vec4 baseTexture = texture(Sampler0, texCoord0);
    if (baseTexture.a < 0.1) discard;

    vec2 uv_noise = (gl_FragCoord.xy / 800.0) * 3.0; 
    vec4 fbmColor = colormap(pattern(uv_noise));
    
    vec3 tintedUI = baseTexture.rgb * (fbmColor.rgb + vec3(0.4)); 
    
    vec3 finalColor = mix(baseTexture.rgb, tintedUI, 0.85);

    fragColor = vec4(finalColor, baseTexture.a) * ColorModulator;
}