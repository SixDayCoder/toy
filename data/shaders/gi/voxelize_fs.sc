$input v_position, v_normal, v_color, v_texcoord0

// #extension GL_ARB_shader_image_load_store : enable

#include <pbr/pbr.sh>
#include <pbr/light.sh>

#include <gi/gi.sh>

#include <encode.sh>
#include <bgfx_compute.sh>

UIMAGE3D_RW(s_voxels_albedo, r32ui, 1);
UIMAGE3D_RW(s_voxels_normals, r32ui, 2);

void main()
{
    if (abs(v_position.z) > VOXELGI_RESOLUTION_Z || abs(v_position.x) > 1.0 || abs(v_position.y) > 1.0) return;
    vec3 position = v_position.xyz * u_voxelgi_extents;
    //position += u_eye_snap;

    Fragment fragment;
	fragment.position = position;
	fragment.normal = normalize(v_normal);
	fragment.uv = v_texcoord0.xy;
	fragment.color = v_color;
    
//#include <pbr/fs_depth.sh>
#include <pbr/fs_normal.sh>

    Material material;
	material.albedo = u_albedo.rgb * sample_material_texture(s_albedo, fragment.uv).rgb;

#include <pbr/fs_emission.sh>

    fragment.color = vec4(material.albedo + emission.rgb, 1.0);
    
    vec3 voxel = v_position.xyz * 0.5 + 0.5; // [-1,1] to [0,1]
    ivec3 coord = ivec3(u_voxelgi_resolution * voxel);
    
    uint color_enc = encodeRGBA8(fragment.color * 255.0);
#if BGFX_SHADER_LANGUAGE_HLSL
    InterlockedMax(s_voxels_albedo.m_texture[coord], color_enc);
#else
    imageAtomicMax(s_voxels_albedo, coord, color_enc);
#endif

    uint normal_enc = encodeNormal(v_normal);
#if BGFX_SHADER_LANGUAGE_HLSL
    InterlockedMax(s_voxels_normals.m_texture[coord], color_enc);
#else
    imageAtomicMax(s_voxels_normals, coord, normal_enc);
#endif
}
