package merklebtree;

import java.util.*;

public class ByteArrayWrapper implements Comparable<ByteArrayWrapper>
{
    public final byte[] data;

    public ByteArrayWrapper(byte[] data)
    {
        if (data == null)
            throw new IllegalArgumentException("Null array!");
        this.data = data;
    }

    @Override
    public int hashCode()
    {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ByteArrayWrapper))
            return false;
        ByteArrayWrapper other = (ByteArrayWrapper) obj;
        if (!Arrays.equals(data, other.data))
            return false;
        return true;
    }

    @Override
    public int compareTo(ByteArrayWrapper o) {
        if (data.length < o.data.length)
            return -1;
        if (data.length > o.data.length)
            return 1;
        for (int i=0; i < data.length; i++)
            if (data[i] != o.data[i])
                return (0xff & data[i]) - (0xff & o.data[i]);
        return 0;
    }

    @Override
    public String toString() {
        return bytesToHex(data);
    }

    public static String bytesToHex(byte[] data)
    {
        StringBuilder s = new StringBuilder();
        for (byte b : data)
            s.append(String.format("%02x", b & 0xFF));
        return s.toString();
    }
}
