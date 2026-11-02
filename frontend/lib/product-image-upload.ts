const MAX_BYTES=8*1024*1024;
const MAX_PIXELS=12_000_000;
export function uploadDimensions(width:number,height:number,bytes:number){
 if(width<1||height<1||!Number.isFinite(width)||!Number.isFinite(height))throw new Error("This photo has invalid dimensions.");
 const resize=bytes>MAX_BYTES||width>8000||height>8000||width*height>MAX_PIXELS;
 const scale=resize?Math.min(1,3200/Math.max(width,height)):1;
 return {width:Math.max(1,Math.round(width*scale)),height:Math.max(1,Math.round(height*scale)),resize};
}

export async function prepareProductImage(file:File):Promise<{file:File;resized:boolean}>{
 if(!/\.(jpe?g|png|webp)$/i.test(file.name)&&!/^image\/(jpeg|png|webp)$/.test(file.type))throw new Error("Choose a JPEG, PNG or WebP photo.");
 if(!file.size)throw new Error("This file is empty. Choose another photo.");
 const url=URL.createObjectURL(file);
 const image=document.createElement("img");
 try{
  image.src=url;
  try{await image.decode();}catch{throw new Error("This photo could not be opened. Export it as JPEG, PNG or WebP and try again.");}
  const target=uploadDimensions(image.naturalWidth,image.naturalHeight,file.size);
  if(!target.resize)return {file,resized:false};
  const canvas=document.createElement("canvas");canvas.width=target.width;canvas.height=target.height;
  const context=canvas.getContext("2d");if(!context)throw new Error("Cannot prepare this photo. Try a smaller image.");
  context.drawImage(image,0,0,canvas.width,canvas.height);
  for(const quality of [.95,.88,.8]){
   const blob=await new Promise<Blob|null>(resolve=>canvas.toBlob(resolve,"image/webp",quality));
   if(blob&&blob.size<=MAX_BYTES){
    const extension=blob.type==="image/webp"?"webp":"png";
    return {file:new File([blob],file.name.replace(/\.[^.]+$/,"")+"."+extension,{type:blob.type}),resized:true};
   }
  }
  throw new Error("This photo is still too large. Export a smaller version under 8 MB.");
 }finally{URL.revokeObjectURL(url);image.src="";}
}
