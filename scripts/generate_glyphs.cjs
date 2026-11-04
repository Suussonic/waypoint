/** Generate map lettering with the official MapLibre font-maker / FreeType WASM. */
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'..'),tool=path.join(root,'tools/glyph-generation');
const {createHash}=require('node:crypto');
for(const [name,sha256] of Object.entries({
 'sdfglyph.cjs':'2d5cce9e20511e3e1798a696b496cfb4fb9c55cd9cceaa73fd77849e1c3d7a64',
 'sdfglyph.wasm':'de2986e66201499de76f21bb3a649e1f27d7d68af21cb2616dc3a7b25bade691'
})) {
 if(createHash('sha256').update(fs.readFileSync(path.join(tool,name))).digest('hex')!==sha256)
  throw new Error(`Unreviewed glyph generator: ${name}. Review provenance before updating the hash.`);
}
const fonts={
 'Waypoint Condensed':'roboto_condensed.ttf',
 'Waypoint Technical':'rajdhani.ttf',
 'Waypoint Pixel':'minecraft_seven.ttf',
 'Waypoint Retro':'monocraft.ttf',
 'Waypoint Borda':'borda_regular.ttf',
 'Waypoint Stratum':'stratum2_regular.ttf',
 'Waypoint Futura':'futura_condensed_medium.ttf',
 'Waypoint Terminal':'share_tech_mono.ttf',
 'Waypoint Atlas':'libre_baskerville.ttf',
 'Waypoint Classic':'noto_sans.ttf',
 'Waypoint Western':'kirsty_regular.ttf'
};
(async()=>{
 const mod={wasmBinary:fs.readFileSync(path.join(tool,'sdfglyph.wasm'))};
 await new Promise((resolve,reject)=>{
  mod.onRuntimeInitialized=resolve;mod.onAbort=reject;
  vm.runInNewContext(fs.readFileSync(path.join(tool,'sdfglyph.cjs'),'utf8'),{Module:mod,require,process,console,__dirname:tool,Buffer,TextDecoder,TextEncoder,setTimeout,clearTimeout},{filename:'sdfglyph.cjs'});
 });
 const create=name=>{const bytes=Buffer.from(name+'\0');const ptr=mod._malloc(bytes.length);mod.HEAPU8.set(bytes,ptr);const stack=mod._create_fontstack(ptr);mod._free(ptr);return stack;};
 for(const [name,file] of Object.entries(fonts)){
  const stack=create(name),ptrs=[];
  for(const f of [file,'noto_sans.ttf']){
   const bytes=fs.readFileSync(path.join(root,'app/src/main/res/font',f));const ptr=mod._malloc(bytes.length);ptrs.push(ptr);mod.HEAPU8.set(bytes,ptr);mod._fontstack_add_face(stack,ptr,bytes.length);
  }
  const dir=path.join(root,'app/src/main/assets/glyphs',name);fs.mkdirSync(dir,{recursive:true});
  // Other scripts retain provider Noto coverage through the native resolver.
  for(const start of [0,256,512,768,1024,1280,1536,1792,2048,8192,8448]){
   const buffer=mod._generate_glyph_buffer(stack,start),ptr=mod._glyph_buffer_data(buffer),len=mod._glyph_buffer_size(buffer);
   fs.writeFileSync(path.join(dir,`${start}-${start+255}.pbf`),Buffer.from(mod.HEAPU8.slice(ptr,ptr+len)));mod._free_glyph_buffer(buffer);
  }
  mod._free_fontstack(stack);for(const p of ptrs)mod._free(p);console.log(name);
 }
})().catch(e=>{console.error(e);process.exitCode=1});
