package com.moulberry.axiom.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class IpAddressMatcher {
   private final int nMaskBits;
   private final InetAddress requiredAddress;

   public IpAddressMatcher(String ipAddress) {
      if (ipAddress.indexOf(47) > 0) {
         String[] addressAndMask = ipAddress.split("/");
         ipAddress = addressAndMask[0];
         this.nMaskBits = Integer.parseInt(addressAndMask[1]);
      } else {
         this.nMaskBits = -1;
      }

      this.requiredAddress = this.parseAddress(ipAddress);

      assert this.requiredAddress.getAddress().length * 8 >= this.nMaskBits : String.format(
         "IP address %s is too short for bitmask of length %d", ipAddress, this.nMaskBits
      );
   }

   public boolean matches(String address) {
      InetAddress remoteAddress = this.parseAddress(address);
      if (!this.requiredAddress.getClass().equals(remoteAddress.getClass())) {
         return false;
      } else if (this.nMaskBits < 0) {
         return remoteAddress.equals(this.requiredAddress);
      } else {
         byte[] remAddr = remoteAddress.getAddress();
         byte[] reqAddr = this.requiredAddress.getAddress();
         int nMaskFullBytes = this.nMaskBits / 8;
         byte finalByte = (byte)(65280 >> (this.nMaskBits & 7));

         for (int i = 0; i < nMaskFullBytes; i++) {
            if (remAddr[i] != reqAddr[i]) {
               return false;
            }
         }

         return finalByte != 0 ? (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte) : true;
      }
   }

   private InetAddress parseAddress(String address) {
      try {
         return InetAddress.getByName(address);
      } catch (UnknownHostException var3) {
         throw new IllegalArgumentException("Failed to parse address" + address, var3);
      }
   }
}
