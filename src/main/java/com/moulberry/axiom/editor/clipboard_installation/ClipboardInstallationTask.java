package com.moulberry.axiom.editor.clipboard_installation;

import com.moulberry.axiom.utils.Authorization;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public interface ClipboardInstallationTask {
   void renderConfirmationPopup();

   void start();

   float progress();

   boolean isFinished();

   Exception getException();

   static int checkStatusCode(URL url) throws IOException {
      HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
      httpURLConnection.setRequestProperty("User-Agent", Authorization.getUserAgent());
      httpURLConnection.setRequestMethod("GET");
      return httpURLConnection.getResponseCode();
   }

   static byte[] downloadWithProgress(URL url, Consumer<Float> progress) throws IOException {
      HttpURLConnection httpURLConnection = null;

      try {
         httpURLConnection = (HttpURLConnection)url.openConnection();
         httpURLConnection.setRequestProperty("User-Agent", Authorization.getUserAgent());
         httpURLConnection.setRequestMethod("GET");
         long completeFileSize = httpURLConnection.getContentLength();
         progress.accept(0.17F);

         byte[] var23;
         try (BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream())) {
            List<byte[]> bufs = null;
            byte[] result = null;
            int total = 0;

            int n;
            do {
               byte[] buf = new byte[16384];
               int nread = 0;

               while ((n = in.read(buf, nread, buf.length - nread)) > 0) {
                  nread += n;
               }

               if (nread > 0) {
                  if (2147483639 - total < nread) {
                     throw new OutOfMemoryError("Required array size too large");
                  }

                  if (nread < buf.length) {
                     buf = Arrays.copyOfRange(buf, 0, nread);
                  }

                  total += nread;
                  if (completeFileSize > 0L) {
                     progress.accept(0.3F + 0.7F * (float)((double)total / completeFileSize));
                  }

                  if (result == null) {
                     result = buf;
                  } else {
                     if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                     }

                     bufs.add(buf);
                  }
               }
            } while (n >= 0);

            if (bufs == null) {
               if (result == null) {
                  return new byte[0];
               }

               return result.length == total ? result : Arrays.copyOf(result, total);
            }

            result = new byte[total];
            int offset = 0;
            int remaining = total;

            for (byte[] b : bufs) {
               int count = Math.min(b.length, remaining);
               System.arraycopy(b, 0, result, offset, count);
               offset += count;
               remaining -= count;
            }

            var23 = result;
         }

         return var23;
      } catch (Exception var17) {
         if (httpURLConnection != null) {
            httpURLConnection.disconnect();
         }

         throw var17;
      }
   }
}
