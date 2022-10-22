package Tools;

import NetworkLayer.NetworkLayer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Translator class that holds some tools created by us for use in the rest of the classes.
 * Some of the methods present here are provided by default by Java.
 * We used this oportunity to further develop our skills and get used to creating tools for our use.
 */
public class Translator {


    /**
     * The method returns a byte array that stores the bytes in that range.
     *
     * @param buffer buffer from which the byte array is created
     * @param from   starting index(0 is the starting index in an array).
     * @param to     end index
     * @param including  boolean set to true if the upper and lower bound will be included.
     * @return the bytes stored in range.
     */
    public static byte[] getBytesInRange(ByteBuffer buffer, int from, int to, boolean including) {
        int size;
        byte[] bytes;
        if (including){
            size = to - from + 1;
            bytes = new byte[size];
            for (int i = from; i<=to;i++ ){
                bytes[i-from] = buffer.get(i);
            }
            return bytes;

        } else{
            size = to - from -1;
            bytes =new byte[size];
            for (int i = from +1 ; i<to;i++){
                bytes[i-from-1] = buffer.get(i);
            }
            return bytes;
        }

    }




    /**
     * method that receives a buffer and returns a byte array containing all the including the starting index.
     *
     * @param buff buffer containing bytes.
     * @param startIndex Index from which the byte array will be copied from the buffer.
     * @return byte array containing the bytes starting with that index.
     */
    public static byte[] returnByteArray(ByteBuffer buff, int startIndex) {
        int size = buff.capacity() - startIndex;
        byte[] bytes = new byte[size];
        for (int i = startIndex;i<buff.capacity();i++){
            bytes[i-startIndex] = buff.get(i);
        }
        return bytes;
    }


    /**
     * Method returns an array containing all the bytes in the buffer
     *
     * @param buffer byte buffer
     * @return byte array containing all the bytes in the buffer
     */
    public static byte[] returnAllByteArray(ByteBuffer buffer) {
       byte[] bytes = new byte[buffer.capacity()];
       for (int i = 0;i < buffer.capacity();i++){
           bytes[i] = buffer.get(i);
       }
       return bytes;
    }

    /**
     * Translates a char array to a string.
     *
     * @param first char array to be translated into a string.
     * @return string representation of the char array.
     */
    public static String charToStringBuilder(char[] first) {
        return new String(first);
    }

    /**
     * Translates two char arrays to a string.
     *
     * @param first  first char array to be appended.
     * @param second second char array to be appended.
     * @return string representation of the two char arrays.
     */
    public static String charToStringBuilder(char[] first, char[] second) {
        String firstString = new String(first);
        String secondString = new String(second);
        return firstString+secondString;
    }

    /**
     * Translates specific parts of the char into a string.
     *
     * @param first char array.
     * @param from  index to start.
     * @param to    index to finish.
     * @return string representation of that specific part of the chararray.
     */
    public static String charToStringBuilder(char[] first, int from, int to) {
            StringBuilder str = new StringBuilder();
            char[] chars;
            for (int i =from;i<to-from+1;i++){
                str.append((first[i]));
            }
            return str.toString();
    }


    /**
     * Byte translator. Translates the byte variable into a char array that holds the
     * binary representation of the byte.
     *
     * @param aByte byte value
     * @return char[] holding the binary representation of the byte.
     */
    public static char[] byteTranslator(byte aByte) {

        String byteToString = String.format("%8s", Integer.toBinaryString(aByte & 0xFF)).replace(' ', '0');
        char[] result = byteToString.toCharArray();


        return result;
    }

    /**
     * The method works by receiving a byte array and creating a string out of it.
     *
     * @param aByte byte array that follows to be transformed into a textual representation.
     * @return string of the form: [USERNAME]: xxxxx.
     */
    public static String byteTranslatorToString(byte[] aByte) {
        String username = new String(aByte);//.split(":");
        String f =  username;
        return f;
    }


    /**
     * Method translates a String into a byte array.
     * @param message String to be translated into a byte array.
     * @return byte array containing the representation of the input string.
     */
    public static byte[] stringToByteArray(String message){
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        return bytes;
    }

    /**
     * Method adds all the bytes from the specified array to a buffer and returns it.
     * @param bytes
     * @return
     */
    public static ByteBuffer addToBuffer(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        for (int i=0; i<bytes.length;i++){
            byte b = bytes[i];

            buffer.put(b);
        }
        return buffer;
    }

    /**
     * Method adds all bytes from the specified array starting with the startingIndex.
     * @param bytes
     * @return
     */
    public static ByteBuffer addToBuffer(byte[] bytes, int startIndex){
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        for (int i=startIndex; i<bytes.length;i++){
            byte b = bytes[i];
            buffer.put(b);
        }
        return buffer;
    }

    /**
     * Method adds all bytes from the specified array that are between the startIndex and endIndex.
     * @param bytes
     * @return
     */
    public static ByteBuffer addToBuffer(byte[] bytes, int startIndex, int endIndex){
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        if (endIndex<bytes.length) {
            for (int i = startIndex; i < endIndex; i++) {
                byte b = bytes[i];
                buffer.put(b);
            }
        } else{
            throw new ArrayIndexOutOfBoundsException("The end index is bigger than the size of the byteArray. \n TranslatorClass");
        }
        return buffer;
    }

    public static byte[] deletePadding(byte[] bytes) {
        ArrayList<Byte> arrayList = new ArrayList<>();
        for (byte aByte : bytes){
            arrayList.add(aByte);
        }
        byte[] ret;
        ArrayList<Byte> arr = new ArrayList<>();
        arr.add((byte) '*');
        arrayList.removeAll(arr);
        ret = new byte[arrayList.size()];
        for (int i = 0;i < arrayList.size();i++){
            ret[i] = arrayList.get(i);
        }
        return ret;

    }
    public static String createString(ByteBuffer byteBuffer){
        byte[] bytes = returnAllByteArray(byteBuffer);
        String s = new String(bytes);

        return s;
    }

}
