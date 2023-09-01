package aces.webctrl.scripts.commissioning.core;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
public class Settings {
  public volatile static Path mainDataFile;
  public volatile static String baseURI = null;
  public synchronized static boolean save(){
    final ByteBuffer buf = ByteBuffer.wrap(baseURI==null?new byte[0]:baseURI.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    try(
      FileChannel out = FileChannel.open(mainDataFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    ){
      while (buf.hasRemaining()){
        out.write(buf);
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
  public synchronized static boolean load(){
    try{
      if (Files.exists(mainDataFile)){
        final byte[] buf = Files.readAllBytes(mainDataFile);
        if (buf.length==0){
          baseURI = null;
        }else{
          baseURI = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
        }
      }
      return true;
    }catch(Throwable t){
      Initializer.log(t);
      return false;
    }
  }
}