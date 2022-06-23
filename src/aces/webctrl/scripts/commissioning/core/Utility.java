/*
  BSD 3-Clause License
  Copyright (c) 2022, Automatic Controls Equipment Systems, Inc.
  Contributors: Cameron Vogt (@cvogt729)
*/
package aces.webctrl.scripts.commissioning.core;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.time.*;
import java.time.format.*;
public class Utility {
  /**
   * Used to convert between time variables and user-friendly strings.
   */
  public final static DateTimeFormatter format = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss").withZone(ZoneId.systemDefault());
  /**
   * A regular expression to match vertical space characters.
   */
  public final static Pattern V_SPACE = Pattern.compile("\\v");
  /**
   * The system default line terminator.
   */
  public final static String NEW_LINE = System.lineSeparator();
  /**
   * @param time should be some value returned by {@code System.currentTimeMillis()}.
   * @return a formatted {@code String} representing the given time.
   */
  public static String getDateString(long time){
    return format.format(Instant.ofEpochMilli(time));
  }
  /**
   * @return a {@code String} containing the stack trace of the given {@code Throwable}.
   */
  public static String getStackTrace(Throwable t){
    StringWriter sw = new StringWriter(128);
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
  /**
   * This method is provided for compatibility with older JRE versions.
   * Newer JREs already have a built-in equivalent of this method: {@code InputStream.readAllBytes()}.
   * @return a {@code byte[]} array containing all remaining bytes read from the {@code InputStream}.
   */
  public static byte[] readAllBytes(InputStream s) throws IOException {
    ArrayList<byte[]> list = new ArrayList<byte[]>();
    int len = 0;
    byte[] buf;
    int read;
    while (true){
      buf = new byte[8192];
      read = s.read(buf);
      if (read==-1){
        break;
      }
      len+=read;
      list.add(buf);
      if (read!=buf.length){
        break;
      }
    }
    byte[] arr = new byte[len];
    int i = 0;
    for (byte[] bytes:list){
      read = Math.min(bytes.length,len);
      len-=read;
      System.arraycopy(bytes, 0, arr, i, read);
      i+=read;
    }
    return arr;
  }
  /**
   * Loads all bytes from the given resource and convert to a {@code UTF-8} string.
   * @return the {@code UTF-8} string representing the given resource.
   */
  public static String loadResourceAsString(String name) throws Throwable {
    byte[] arr;
    try(
      InputStream s = Utility.class.getClassLoader().getResourceAsStream(name);
    ){
      arr = readAllBytes(s);
    }
    return new String(arr, java.nio.charset.StandardCharsets.UTF_8);
  }
  /**
   * Encodes a string to be parsed as a list.
   * Intended to be used to encode AJAX responses.
   * Escapes semi-colons and backslashes using the backslash character.
   */
  public static String encodeAJAX(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      if (c=='\\' || c==';'){
        sb.append('\\');
      }
      sb.append(c);
    }
    return sb.toString();
  }
  /**
   * Escapes a {@code String} for usage in HTML attribute values.
   * @param str is the {@code String} to escape.
   * @return the escaped {@code String}.
   */
  public static String escapeHTML(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    int j;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      j = c;
      if (j>=32 && j<127){
        switch (c){
          case '&':{
            sb.append("&amp;");
            break;
          }
          case '"':{
            sb.append("&quot;");
            break;
          }
          case '\'':{
            sb.append("&apos;");
            break;
          }
          case '<':{
            sb.append("&lt;");
            break;
          }
          case '>':{
            sb.append("&gt;");
            break;
          }
          default:{
            sb.append(c);
          }
        }
      }else if (j<1114111 && (j<=55296 || j>57343)){
        sb.append("&#").append(Integer.toString(j)).append(";");
      }
    }
    return sb.toString();
  }
  /**
   * Intended to escape strings for use in Javascript.
   * Escapes backslashes, single quotes, and double quotes.
   * Replaces new-line characters with the corresponding escape sequences.
   */
  public static String escapeJS(String str){
    int len = str.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    for (int i=0;i<len;++i){
      c = str.charAt(i);
      switch (c){
        case '\\': case '\'': case '"': {
          sb.append('\\').append(c);
          break;
        }
        case '\n': {
          sb.append("\\n");
          break;
        }
        case '\t': {
          sb.append("\\t");
          break;
        }
        case '\r': {
          sb.append("\\r");
          break;
        }
        case '\b': {
          sb.append("\\b");
          break;
        }
        case '\f': {
          sb.append("\\f");
          break;
        }
        default: {
          sb.append(c);
        }
      }
    }
    return sb.toString();
  }
  /**
   * Reverses the order and XORs each character with 4.
   * The array is modified in-place, so no copies are made.
   * For convenience, the given array is returned.
   */
  public static char[] obfuscate(char[] arr){
    char c;
    for (int i=0,j=arr.length-1;i<=j;++i,--j){
      if (i==j){
        arr[i]^=4;
      }else{
        c = (char)(arr[j]^4);
        arr[j] = (char)(arr[i]^4);
        arr[i] = c;
      }
    }
    return arr;
  }
  /**
   * Converts a character array into a byte array.
   */
  public static byte[] toBytes(char[] arr){
    return java.nio.charset.StandardCharsets.UTF_8.encode(java.nio.CharBuffer.wrap(arr)).array();
  }
}