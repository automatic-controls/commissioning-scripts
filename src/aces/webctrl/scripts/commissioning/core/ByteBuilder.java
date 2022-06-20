package aces.webctrl.scripts.commissioning.core;
import java.util.*;
public class ByteBuilder {
  private ArrayList<byte[]> list;
  private int len = 0;
  public ByteBuilder(){
    list = new ArrayList<byte[]>();
  }
  public ByteBuilder(int capacity){
    list = new ArrayList<byte[]>(capacity);
  }
  public void write(String str){
    if (str==null){
      str = "";
    }
    write(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }
  public void write(boolean x){
    SerializationStream s = new SerializationStream(1);
    s.write(x);
    writeRaw(s.data);
  }
  public void write(double x){
    SerializationStream s = new SerializationStream(8);
    s.write(x);
    writeRaw(s.data);
  }
  public void write(long x){
    SerializationStream s = new SerializationStream(8);
    s.write(x);
    writeRaw(s.data);
  }
  public void write(int x){
    SerializationStream s = new SerializationStream(4);
    s.write(x);
    writeRaw(s.data);
  }
  public void write(byte[] arr){
    write(arr.length);
    writeRaw(arr);
  }
  public void writeRaw(byte[] arr){
    list.add(arr);
    len+=arr.length;
  }
  public byte[] compute(){
    SerializationStream s = new SerializationStream(len);
    for (byte[] arr:list){
      s.writeRaw(arr);
    }
    return s.data;
  }
}