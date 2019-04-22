package merklebtree;

import java.security.*;
import java.util.*;

public class RAMStorage implements ContentAddressedStorage {
    private Map<ByteArrayWrapper, byte[]> storage = new HashMap<>();

    @Override
    public byte[] put(byte[] value) {
        byte[] hash = hash(value);
        storage.put(new ByteArrayWrapper(hash), value);
        return hash;
    }

    @Override
    public byte[] get(byte[] key) {
        return storage.get(new ByteArrayWrapper(key));
    }

    @Override
    public void remove(byte[] key) {
        storage.remove(new ByteArrayWrapper(key));
    }

    public void clear() {
        storage.clear();
    }

    public int size() {
        return storage.size();
    }

    public static byte[] hash(byte[] input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input);
            return md.digest();
        } catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("couldn't find hash algorithm");
        }
    }
}
