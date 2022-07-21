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
  private final static Pattern formatter = Pattern.compile("\\$(\\d)");
  public static String format(final String s, final Object... args){
    final String[] args_ = new String[args.length];
    for (int i=0;i<args.length;++i){
      args_[i] = args[i]==null?"":Matcher.quoteReplacement(args[i].toString());
    }
    return formatter.matcher(s).replaceAll(new java.util.function.Function<MatchResult,String>(){
      public String apply(MatchResult m){
        int i = Integer.parseInt(m.group(1));
        return i<args.length?args_[i]:"";
      }
    });
  }
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
   * @return a string which encodes the given list.
   * @see #decodeList(String)
   */
  public static String encodeList(List<String> list){
    int cap = list.size()<<2;
    for (String s:list){
      cap+=s.length();
    }
    StringBuilder sb = new StringBuilder(cap);
    for (String s:list){
      sb.append(s.replace("\\", "\\\\").replace(";", "\\;")).append(';');
    }
    return sb.toString();
  }
  /**
   * @return a list decoded from the given string.
   * @see #encodeList(List)
   */
  public static ArrayList<String> decodeList(String s){
    int len = s.length();
    int i,j,k,max=0;
    char c;
    boolean esc = false;
    for (i=0,j=0,k=0;i<len;++i){
      if (esc){
        esc = false;
        ++k;
      }else{
        c = s.charAt(i);
        if (c=='\\'){
          esc = true;
        }else if (c==';'){
          ++j;
          if (k>max){
            max = k;
          }
          k = 0;
        }else{
          ++k;
        }
      }
    }
    ArrayList<String> list = new ArrayList<String>(j);
    StringBuilder sb = new StringBuilder(max);
    esc = false;
    for (i=0;i<len;++i){
      c = s.charAt(i);
      if (esc){
        esc = false;
        sb.append(c);
      }else if (c=='\\'){
        esc = true;
      }else if (c==';'){
        list.add(sb.toString());
        sb.setLength(0);
      }else{
        sb.append(c);
      }
    }
    return list;
  }
  /**
   * Escapes a {@code String} for usage in CSV document cells.
   * @param str is the {@code String} to escape.
   * @return the escaped {@code String}.
   */
  public static String escapeCSV(String str){
    if (str.indexOf(',')==-1 && str.indexOf('"')==-1 && str.indexOf('\n')==-1 && str.indexOf('\r')==-1){
      return str;
    }else{
      return '"'+str.replace("\"","\"\"")+'"';
    }
  }
  /**
   * Escapes a {@code String} for usage in HTML attribute values.
   * @param str is the {@code String} to escape.
   * @return the escaped {@code String}.
   */
  public static String escapeHTML(CharSequence str){
    if (str==null){ return ""; }
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
    if (str==null){ return ""; }
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
   * Encodes a JSON string.
   */
  public static String escapeJSON(String s){
    if (s==null){ return "NULL"; }
    int len = s.length();
    StringBuilder sb = new StringBuilder(len+16);
    char c;
    String hex;
    int hl;
    for (int i=0;i<len;++i){
      c = s.charAt(i);
      switch (c){
        case '\\': case '/': case '"': {
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
          if (c>31 && c<127){
            sb.append(c);
          }else{
            //JDK17: hex = HexFormat.of().toHexDigits(c);
            hex = Integer.toHexString((int)c);
            hl = hex.length();
            if (hl<=4){
              sb.append("\\u");
              for (;hl<4;hl++){
                sb.append('0');
              }
              sb.append(hex);
            }
          }
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