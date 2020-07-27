package com.kaist.gaenclient;

import android.os.ParcelUuid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Utils {
    public static class UUIDConvert {
        /** Length of bytes for 16 bit UUID */
        final static int UUID_BYTES_16_BIT = 2;
        /** Length of bytes for 32 bit UUID */
        final static int UUID_BYTES_32_BIT = 4;
        /** Length of bytes for 128 bit UUID */
        final static int UUID_BYTES_128_BIT = 16;
        final static ParcelUuid BASE_UUID =
                ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

        /**
         * Parse UUID from bytes. The {@code uuidBytes} can represent a 16-bit, 32-bit or 128-bit UUID,
         * but the returned UUID is always in 128-bit format.
         * Note UUID is little endian in Bluetooth.
         *
         * @param uuidBytes Byte representation of uuid.
         * @return {@link ParcelUuid} parsed from bytes.
         * @throws IllegalArgumentException If the {@code uuidBytes} cannot be parsed.
         *
         * Copied from java/android/bluetooth/BluetoothUuid.java
         * Copyright (C) 2009 The Android Open Source Project
         * Licensed under the Apache License, Version 2.0
         */
        public static ParcelUuid parseUuidFrom(byte[] uuidBytes) {
            if (uuidBytes == null) {
                throw new IllegalArgumentException("uuidBytes cannot be null");
            }
            int length = uuidBytes.length;
            if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT &&
                    length != UUID_BYTES_128_BIT) {
                throw new IllegalArgumentException("uuidBytes length invalid - " + length);
            }
            // Construct a 128 bit UUID.
            if (length == UUID_BYTES_128_BIT) {
                ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
                long msb = buf.getLong(8);
                long lsb = buf.getLong(0);
                return new ParcelUuid(new UUID(msb, lsb));
            }
            // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
            // 128_bit_value = uuid * 2^96 + BASE_UUID
            long shortUuid;
            if (length == UUID_BYTES_16_BIT) {
                shortUuid = uuidBytes[0] & 0xFF;
                shortUuid += (uuidBytes[1] & 0xFF) << 8;
            } else {
                shortUuid = uuidBytes[0] & 0xFF ;
                shortUuid += (uuidBytes[1] & 0xFF) << 8;
                shortUuid += (uuidBytes[2] & 0xFF) << 16;
                shortUuid += (uuidBytes[3] & 0xFF) << 24;
            }
            long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
            long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
            return new ParcelUuid(new UUID(msb, lsb));
        }

        public static ParcelUuid convertShortToParcelUuid(int shortUuid) {
            return parseUuidFrom(new byte[] {
                    (byte)(shortUuid & 0xff), (byte)((shortUuid >> 8) & 0xff)
            });
        }

        public static int convertParcelUuidToShort(ParcelUuid parcelUuid) {
            UUID uuid = parcelUuid.getUuid();
            long shortUuid = (uuid.getMostSignificantBits() - BASE_UUID.getUuid().getMostSignificantBits()) >> 32;
            return (int)(shortUuid & 0xffff);
        }

        // Assume big-endianness.
        public static UUID asUuid(byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            long firstLong = bb.getLong();
            long secondLong = bb.getLong();
            return new UUID(firstLong, secondLong);
        }

        public static byte[] asBytes(UUID uuid) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());
            return bb.array();
        }
    }

    /* UUID v3 / v5 creator.
     * Reference: https://stackoverflow.com/a/63087679
     */
    public static class HashUuidCreator {
        // Domain Name System
        public static final UUID NAMESPACE_DNS = new UUID(0x6ba7b8109dad11d1L, 0x80b400c04fd430c8L);
        // Uniform Resource Locator
        public static final UUID NAMESPACE_URL = new UUID(0x6ba7b8119dad11d1L, 0x80b400c04fd430c8L);
        // ISO Object ID
        public static final UUID NAMESPACE_ISO_OID = new UUID(0x6ba7b8129dad11d1L, 0x80b400c04fd430c8L);
        // X.500 Distinguished Name
        public static final UUID NAMESPACE_X500_DN = new UUID(0x6ba7b8149dad11d1L, 0x80b400c04fd430c8L);

        private static final int VERSION_3 = 3; // UUIDv3 MD5
        private static final int VERSION_5 = 5; // UUIDv5 SHA1

        private static final String MESSAGE_DIGEST_MD5 = "MD5"; // UUIDv3
        private static final String MESSAGE_DIGEST_SHA1 = "SHA-1"; // UUIDv5

        private static UUID getHashUuid(UUID namespace, String name, String algorithm, int version) {
            final byte[] hash;
            final MessageDigest hasher;

            try {
                // Instantiate a message digest for the chosen algorithm
                hasher = MessageDigest.getInstance(algorithm);

                // Insert name space if NOT NULL
                if (namespace != null) {
                    hasher.update(toBytes(namespace.getMostSignificantBits()));
                    hasher.update(toBytes(namespace.getLeastSignificantBits()));
                }

                // Generate the hash
                hash = hasher.digest(name.getBytes(StandardCharsets.UTF_8));

                // Split the hash into two parts: MSB and LSB
                long msb = toNumber(hash, 0, 8); // first 8 bytes for MSB
                long lsb = toNumber(hash, 8, 16); // last 8 bytes for LSB

                // Apply version and variant bits (required for RFC-4122 compliance)
                msb = (msb & 0xffffffffffff0fffL) | (version & 0x0f) << 12; // apply version bits
                lsb = (lsb & 0x3fffffffffffffffL) | 0x8000000000000000L; // apply variant bits

                // Return the UUID
                return new UUID(msb, lsb);

            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Message digest algorithm not supported.");
            }
        }

        public static UUID getMd5Uuid(String string) {
            return getHashUuid(null, string, MESSAGE_DIGEST_MD5, VERSION_3);
        }

        public static UUID getSha1Uuid(String string) {
            return getHashUuid(null, string, MESSAGE_DIGEST_SHA1, VERSION_5);
        }

        public static UUID getMd5Uuid(UUID namespace, String string) {
            return getHashUuid(namespace, string, MESSAGE_DIGEST_MD5, VERSION_3);
        }

        public static UUID getSha1Uuid(UUID namespace, String string) {
            return getHashUuid(namespace, string, MESSAGE_DIGEST_SHA1, VERSION_5);
        }

        private static byte[] toBytes(final long number) {
            return new byte[] { (byte) (number >>> 56), (byte) (number >>> 48), (byte) (number >>> 40),
                    (byte) (number >>> 32), (byte) (number >>> 24), (byte) (number >>> 16), (byte) (number >>> 8),
                    (byte) (number) };
        }

        private static long toNumber(final byte[] bytes, final int start, final int length) {
            long result = 0;
            for (int i = start; i < length; i++) {
                result = (result << 8) | (bytes[i] & 0xff);
            }
            return result;
        }
    }

    // Opens, reads and parses a CSV file from given InputStream.
    // Reference: https://stackoverflow.com/a/38415815
    public static class CSVFile {
        InputStream inputStream;

        public CSVFile(InputStream inputStream){
            this.inputStream = inputStream;
        }

        public List<String[]> read() throws RuntimeException {
            List<String[]> resultList = new ArrayList<String[]>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String csvLine;
                while ((csvLine = reader.readLine()) != null) {
                    String[] row = csvLine.split(",");
                    resultList.add(row);
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Error in reading CSV file: " + e);
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    //noinspection ThrowFromFinallyBlock
                    throw new RuntimeException("Error while closing input stream: " + e);
                }
            }
            return resultList;
        }
    }
}
