package com.moulberry.axiom.editor;

import imgui.moulberry92.ImDrawData;
import imgui.moulberry92.ImFontAtlas;
import imgui.moulberry92.ImGui;
import imgui.moulberry92.ImGuiIO;
import imgui.moulberry92.ImGuiViewport;
import imgui.moulberry92.ImVec2;
import imgui.moulberry92.ImVec4;
import imgui.moulberry92.callback.ImPlatformFuncViewport;
import imgui.moulberry92.type.ImInt;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33C;

public final class CustomImGuiImplGl3 {
   private int glVersion = 0;
   private String glslVersion = "";
   private int gFontTexture = -1;
   private int gShaderHandle = 0;
   private int gVertHandle = 0;
   private int gFragHandle = 0;
   private int gAttribLocationTex = 0;
   private int gAttribLocationProjMtx = 0;
   private int gAttribLocationVtxPos = 0;
   private int gAttribLocationVtxUV = 0;
   private int gAttribLocationVtxColor = 0;
   private int gVboHandle = 0;
   private int gElementsHandle = 0;
   private int gVertexArrayObjectHandle = 0;
   private final ImVec2 displaySize = new ImVec2();
   private final ImVec2 framebufferScale = new ImVec2();
   private final ImVec2 displayPos = new ImVec2();
   private final ImVec4 clipRect = new ImVec4();
   private final float[] orthoProjMatrix = new float[16];
   private final int[] lastActiveTexture = new int[1];
   private final int[] lastProgram = new int[1];
   private final int[] lastTexture = new int[1];
   private final int[] lastArrayBuffer = new int[1];
   private final int[] lastVertexArrayObject = new int[1];
   private final int[] lastFramebuffer = new int[1];
   private final int[] lastViewport = new int[4];
   private final int[] lastScissorBox = new int[4];
   private final int[] lastBlendSrcRgb = new int[1];
   private final int[] lastBlendDstRgb = new int[1];
   private final int[] lastBlendSrcAlpha = new int[1];
   private final int[] lastBlendDstAlpha = new int[1];
   private final int[] lastBlendEquationRgb = new int[1];
   private final int[] lastBlendEquationAlpha = new int[1];
   private boolean lastEnableBlend = false;
   private boolean lastEnableCullFace = false;
   private boolean lastEnableDepthTest = false;
   private boolean lastEnableStencilTest = false;
   private boolean lastEnableScissorTest = false;

   public void init() {
      this.init(null);
   }

   public void init(String glslVersion) {
      this.readGlVersion();
      this.setupBackendCapabilitiesFlags();
      if (glslVersion == null) {
         this.glslVersion = "#version 130";
      } else {
         this.glslVersion = glslVersion;
      }

      this.createDeviceObjects();
      if (EditorUI.getIO().hasConfigFlags(1024)) {
         this.initPlatformInterface();
      }
   }

   public void renderDrawData(ImDrawData drawData) {
      if (drawData.getCmdListsCount() > 0) {
         drawData.getDisplaySize(this.displaySize);
         drawData.getDisplayPos(this.displayPos);
         drawData.getFramebufferScale(this.framebufferScale);
         float clipOffX = this.displayPos.x;
         float clipOffY = this.displayPos.y;
         float clipScaleX = this.framebufferScale.x;
         float clipScaleY = this.framebufferScale.y;
         int fbWidth = (int)(this.displaySize.x * this.framebufferScale.x);
         int fbHeight = (int)(this.displaySize.y * this.framebufferScale.y);
         if (fbWidth > 0 && fbHeight > 0) {
            this.backupGlState();
            this.bind(fbWidth, fbHeight);

            for (int cmdListIdx = 0; cmdListIdx < drawData.getCmdListsCount(); cmdListIdx++) {
               GL32.glBufferData(34962, drawData.getCmdListVtxBufferData(cmdListIdx), 35040);
               GL32.glBufferData(34963, drawData.getCmdListIdxBufferData(cmdListIdx), 35040);

               for (int cmdBufferIdx = 0; cmdBufferIdx < drawData.getCmdListCmdBufferSize(cmdListIdx); cmdBufferIdx++) {
                  drawData.getCmdListCmdBufferClipRect(this.clipRect, cmdListIdx, cmdBufferIdx);
                  float clipMinX = (this.clipRect.x - clipOffX) * clipScaleX;
                  float clipMinY = (this.clipRect.y - clipOffY) * clipScaleY;
                  float clipMaxX = (this.clipRect.z - clipOffX) * clipScaleX;
                  float clipMaxY = (this.clipRect.w - clipOffY) * clipScaleY;
                  if (!(clipMaxX <= clipMinX) && !(clipMaxY <= clipMinY)) {
                     GL32.glScissor((int)clipMinX, (int)(fbHeight - clipMaxY), (int)(clipMaxX - clipMinX), (int)(clipMaxY - clipMinY));
                     long textureId = drawData.getCmdListCmdBufferTextureId(cmdListIdx, cmdBufferIdx);
                     int elemCount = drawData.getCmdListCmdBufferElemCount(cmdListIdx, cmdBufferIdx);
                     int idxBufferOffset = drawData.getCmdListCmdBufferIdxOffset(cmdListIdx, cmdBufferIdx);
                     int vtxBufferOffset = drawData.getCmdListCmdBufferVtxOffset(cmdListIdx, cmdBufferIdx);
                     long indices = (long)idxBufferOffset * ImDrawData.sizeOfImDrawIdx();
                     int type = ImDrawData.sizeOfImDrawIdx() == 2 ? 5123 : 5125;
                     GL32.glBindTexture(3553, (int)textureId);
                     if (this.glVersion >= 320) {
                        GL32.glDrawElementsBaseVertex(4, elemCount, type, indices, vtxBufferOffset);
                     } else {
                        GL32.glDrawElements(4, elemCount, type, indices);
                     }
                  }
               }
            }

            this.unbind();
            this.restoreModifiedGlState();
         }
      }
   }

   public void dispose() {
      GL32.glDeleteBuffers(this.gVboHandle);
      GL32.glDeleteBuffers(this.gElementsHandle);
      GL32.glDetachShader(this.gShaderHandle, this.gVertHandle);
      GL32.glDetachShader(this.gShaderHandle, this.gFragHandle);
      GL32.glDeleteProgram(this.gShaderHandle);
      GL32.glDeleteTextures(this.gFontTexture);
      this.shutdownPlatformInterface();
   }

   public void updateFontsTexture() {
      if (this.gFontTexture != -1) {
         GL32.glDeleteTextures(this.gFontTexture);
      }

      ImFontAtlas fontAtlas = EditorUI.getIO().getFonts();
      ImInt width = new ImInt();
      ImInt height = new ImInt();
      ByteBuffer buffer = fontAtlas.getTexDataAsRGBA32(width, height);
      int previousTexture = GL32.glGetInteger(32873);
      this.gFontTexture = GL32.glGenTextures();
      GL32.glActiveTexture(33984);
      GL32.glBindTexture(3553, this.gFontTexture);
      GL32.glTexParameteri(3553, 10241, 9729);
      GL32.glTexParameteri(3553, 10240, 9729);
      GL32.glTexParameteri(3553, 10242, 33071);
      GL32.glTexParameteri(3553, 10243, 33071);
      GL32.glPixelStorei(3317, 1);
      GL32.glPixelStorei(3314, 0);
      GL32.glPixelStorei(3316, 0);
      GL32.glPixelStorei(3315, 0);
      GL32.glTexImage2D(3553, 0, 6408, width.get(), height.get(), 0, 6408, 5121, buffer);
      fontAtlas.setTexID(this.gFontTexture);
      GL32.glBindTexture(3553, previousTexture);
   }

   private void readGlVersion() {
      int[] major = new int[1];
      int[] minor = new int[1];
      GL32.glGetIntegerv(33307, major);
      GL32.glGetIntegerv(33308, minor);
      this.glVersion = major[0] * 100 + minor[0] * 10;
   }

   private void setupBackendCapabilitiesFlags() {
      ImGuiIO io = EditorUI.getIO();
      io.setBackendRendererName("imgui_java_impl_opengl3");
      if (this.glVersion >= 320) {
         io.addBackendFlags(8);
      }

      io.addBackendFlags(1024);
   }

   private void createDeviceObjects() {
      int[] lastTexture = new int[1];
      int[] lastArrayBuffer = new int[1];
      int[] lastVertexArray = new int[1];
      GL32.glGetIntegerv(32873, lastTexture);
      GL32.glGetIntegerv(34964, lastArrayBuffer);
      GL32.glGetIntegerv(34229, lastVertexArray);
      this.createShaders();
      this.gAttribLocationTex = GL32.glGetUniformLocation(this.gShaderHandle, "Texture");
      this.gAttribLocationProjMtx = GL32.glGetUniformLocation(this.gShaderHandle, "ProjMtx");
      this.gAttribLocationVtxPos = GL32.glGetAttribLocation(this.gShaderHandle, "Position");
      this.gAttribLocationVtxUV = GL32.glGetAttribLocation(this.gShaderHandle, "UV");
      this.gAttribLocationVtxColor = GL32.glGetAttribLocation(this.gShaderHandle, "Color");
      this.gVboHandle = GL32.glGenBuffers();
      this.gElementsHandle = GL32.glGenBuffers();
      this.updateFontsTexture();
      GL32.glBindTexture(3553, lastTexture[0]);
      GL32.glBindBuffer(34962, lastArrayBuffer[0]);
      GL32.glBindVertexArray(lastVertexArray[0]);
   }

   private void createShaders() {
      int glslVersionValue = this.parseGlslVersionString();
      CharSequence vertShaderSource;
      CharSequence fragShaderSource;
      if (glslVersionValue < 130) {
         vertShaderSource = this.getVertexShaderGlsl120();
         fragShaderSource = this.getFragmentShaderGlsl120();
      } else if (glslVersionValue == 300) {
         vertShaderSource = this.getVertexShaderGlsl300es();
         fragShaderSource = this.getFragmentShaderGlsl300es();
      } else if (glslVersionValue >= 410) {
         vertShaderSource = this.getVertexShaderGlsl410Core();
         fragShaderSource = this.getFragmentShaderGlsl410Core();
      } else {
         vertShaderSource = this.getVertexShaderGlsl130();
         fragShaderSource = this.getFragmentShaderGlsl130();
      }

      this.gVertHandle = this.createAndCompileShader(35633, vertShaderSource);
      this.gFragHandle = this.createAndCompileShader(35632, fragShaderSource);
      this.gShaderHandle = GL32.glCreateProgram();
      GL32.glAttachShader(this.gShaderHandle, this.gVertHandle);
      GL32.glAttachShader(this.gShaderHandle, this.gFragHandle);
      GL32.glLinkProgram(this.gShaderHandle);
      if (GL32.glGetProgrami(this.gShaderHandle, 35714) == 0) {
         throw new IllegalStateException("Failed to link shader program:\n" + GL32.glGetProgramInfoLog(this.gShaderHandle));
      }
   }

   private int parseGlslVersionString() {
      Pattern p = Pattern.compile("\\d+");
      Matcher m = p.matcher(this.glslVersion);
      if (m.find()) {
         return Integer.parseInt(m.group());
      } else {
         throw new IllegalArgumentException("Invalid GLSL version string: " + this.glslVersion);
      }
   }

   private void backupGlState() {
      GL32.glGetIntegerv(34016, this.lastActiveTexture);
      GL32.glActiveTexture(33984);
      GL32.glGetIntegerv(35725, this.lastProgram);
      GL32.glGetIntegerv(32873, this.lastTexture);
      GL32.glGetIntegerv(34964, this.lastArrayBuffer);
      GL32.glGetIntegerv(34229, this.lastVertexArrayObject);
      GL32.glGetIntegerv(36006, this.lastFramebuffer);
      GL32.glGetIntegerv(2978, this.lastViewport);
      GL32.glGetIntegerv(3088, this.lastScissorBox);
      GL32.glGetIntegerv(32969, this.lastBlendSrcRgb);
      GL32.glGetIntegerv(32968, this.lastBlendDstRgb);
      GL32.glGetIntegerv(32971, this.lastBlendSrcAlpha);
      GL32.glGetIntegerv(32970, this.lastBlendDstAlpha);
      GL32.glGetIntegerv(32777, this.lastBlendEquationRgb);
      GL32.glGetIntegerv(34877, this.lastBlendEquationAlpha);
      this.lastEnableBlend = GL32.glIsEnabled(3042);
      this.lastEnableCullFace = GL32.glIsEnabled(2884);
      this.lastEnableDepthTest = GL32.glIsEnabled(2929);
      this.lastEnableStencilTest = GL32.glIsEnabled(2960);
      this.lastEnableScissorTest = GL32.glIsEnabled(3089);
   }

   private void restoreModifiedGlState() {
      GL32.glUseProgram(this.lastProgram[0]);
      GL32.glBindTexture(3553, this.lastTexture[0]);
      GL32.glActiveTexture(this.lastActiveTexture[0]);
      GL32.glBindVertexArray(this.lastVertexArrayObject[0]);
      GL32.glBindFramebuffer(36160, this.lastFramebuffer[0]);
      GL32.glBindBuffer(34962, this.lastArrayBuffer[0]);
      GL32.glBlendEquationSeparate(this.lastBlendEquationRgb[0], this.lastBlendEquationAlpha[0]);
      GL32.glBlendFuncSeparate(this.lastBlendSrcRgb[0], this.lastBlendDstRgb[0], this.lastBlendSrcAlpha[0], this.lastBlendDstAlpha[0]);
      if (this.lastEnableBlend) {
         GL32.glEnable(3042);
      } else {
         GL32.glDisable(3042);
      }

      if (this.lastEnableCullFace) {
         GL32.glEnable(2884);
      } else {
         GL32.glDisable(2884);
      }

      if (this.lastEnableDepthTest) {
         GL32.glEnable(2929);
      } else {
         GL32.glDisable(2929);
      }

      if (this.lastEnableStencilTest) {
         GL32.glEnable(2960);
      } else {
         GL32.glDisable(2960);
      }

      if (this.lastEnableScissorTest) {
         GL32.glEnable(3089);
      } else {
         GL32.glDisable(3089);
      }

      GL32.glViewport(this.lastViewport[0], this.lastViewport[1], this.lastViewport[2], this.lastViewport[3]);
      GL32.glScissor(this.lastScissorBox[0], this.lastScissorBox[1], this.lastScissorBox[2], this.lastScissorBox[3]);
   }

   private void bind(int fbWidth, int fbHeight) {
      this.gVertexArrayObjectHandle = GL32.glGenVertexArrays();
      GL32.glEnable(3042);
      GL32.glBlendEquation(32774);
      GL32.glBlendFuncSeparate(770, 771, 1, 771);
      GL32.glDisable(2884);
      GL32.glDisable(2929);
      GL32.glDisable(2960);
      GL32.glEnable(3089);
      GL33C.glBindSampler(0, 0);
      GL32.glViewport(0, 0, fbWidth, fbHeight);
      float left = this.displayPos.x;
      float right = this.displayPos.x + this.displaySize.x;
      float top = this.displayPos.y;
      float bottom = this.displayPos.y + this.displaySize.y;
      this.orthoProjMatrix[0] = 2.0F / (right - left);
      this.orthoProjMatrix[5] = 2.0F / (top - bottom);
      this.orthoProjMatrix[10] = -1.0F;
      this.orthoProjMatrix[12] = (right + left) / (left - right);
      this.orthoProjMatrix[13] = (top + bottom) / (bottom - top);
      this.orthoProjMatrix[15] = 1.0F;
      GL32.glUseProgram(this.gShaderHandle);
      GL32.glUniform1i(this.gAttribLocationTex, 0);
      GL32.glUniformMatrix4fv(this.gAttribLocationProjMtx, false, this.orthoProjMatrix);
      GL32.glBindVertexArray(this.gVertexArrayObjectHandle);
      GL32.glBindFramebuffer(36160, 0);
      GL32.glBindBuffer(34962, this.gVboHandle);
      GL32.glBindBuffer(34963, this.gElementsHandle);
      GL32.glEnableVertexAttribArray(this.gAttribLocationVtxPos);
      GL32.glEnableVertexAttribArray(this.gAttribLocationVtxUV);
      GL32.glEnableVertexAttribArray(this.gAttribLocationVtxColor);
      int sizeOfImDrawVert = ImDrawData.sizeOfImDrawVert();
      GL32.glVertexAttribPointer(this.gAttribLocationVtxPos, 2, 5126, false, sizeOfImDrawVert, 0L);
      GL32.glVertexAttribPointer(this.gAttribLocationVtxUV, 2, 5126, false, sizeOfImDrawVert, 8L);
      GL32.glVertexAttribPointer(this.gAttribLocationVtxColor, 4, 5121, true, sizeOfImDrawVert, 16L);
   }

   private void unbind() {
      GL32.glDeleteVertexArrays(this.gVertexArrayObjectHandle);
   }

   private void initPlatformInterface() {
      ImGui.getPlatformIO().setRendererRenderWindow(new ImPlatformFuncViewport() {
         public void accept(ImGuiViewport vp) {
            if (!vp.hasFlags(256)) {
               GL32.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
               GL32.glClear(16384);
            }

            CustomImGuiImplGl3.this.renderDrawData(vp.getDrawData());
         }
      });
   }

   private void shutdownPlatformInterface() {
      ImGui.destroyPlatformWindows();
   }

   private int createAndCompileShader(int type, CharSequence source) {
      int id = GL32.glCreateShader(type);
      GL32.glShaderSource(id, source);
      GL32.glCompileShader(id);
      if (GL32.glGetShaderi(id, 35713) == 0) {
         throw new IllegalStateException("Failed to compile shader:\n" + GL32.glGetShaderInfoLog(id));
      } else {
         return id;
      }
   }

   private String getVertexShaderGlsl120() {
      return this.glslVersion
         + "\nuniform mat4 ProjMtx;\nattribute vec2 Position;\nattribute vec2 UV;\nattribute vec4 Color;\nvarying vec2 Frag_UV;\nvarying vec4 Frag_Color;\nvoid main()\n{\n    Frag_UV = UV;\n    Frag_Color = Color;\n    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n}\n";
   }

   private String getVertexShaderGlsl130() {
      return this.glslVersion
         + "\nuniform mat4 ProjMtx;\nin vec2 Position;\nin vec2 UV;\nin vec4 Color;\nout vec2 Frag_UV;\nout vec4 Frag_Color;\nvoid main()\n{\n    Frag_UV = UV;\n    Frag_Color = Color;\n    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n}\n";
   }

   private String getVertexShaderGlsl300es() {
      return this.glslVersion
         + "\nprecision highp float;\nlayout (location = 0) in vec2 Position;\nlayout (location = 1) in vec2 UV;\nlayout (location = 2) in vec4 Color;\nuniform mat4 ProjMtx;\nout vec2 Frag_UV;\nout vec4 Frag_Color;\nvoid main()\n{\n    Frag_UV = UV;\n    Frag_Color = Color;\n    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n}\n";
   }

   private String getVertexShaderGlsl410Core() {
      return this.glslVersion
         + "\nlayout (location = 0) in vec2 Position;\nlayout (location = 1) in vec2 UV;\nlayout (location = 2) in vec4 Color;\nuniform mat4 ProjMtx;\nout vec2 Frag_UV;\nout vec4 Frag_Color;\nvoid main()\n{\n    Frag_UV = UV;\n    Frag_Color = Color;\n    gl_Position = ProjMtx * vec4(Position.xy,0,1);\n}\n";
   }

   private String getFragmentShaderGlsl120() {
      return this.glslVersion
         + "\n#ifdef GL_ES\n    precision mediump float;\n#endif\nuniform sampler2D Texture;\nvarying vec2 Frag_UV;\nvarying vec4 Frag_Color;\nvoid main()\n{\n    gl_FragColor = Frag_Color * texture2D(Texture, Frag_UV.st);\n}\n";
   }

   private String getFragmentShaderGlsl130() {
      return this.glslVersion
         + "\nuniform sampler2D Texture;\nin vec2 Frag_UV;\nin vec4 Frag_Color;\nout vec4 Out_Color;\nvoid main()\n{\n    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n}\n";
   }

   private String getFragmentShaderGlsl300es() {
      return this.glslVersion
         + "\nprecision mediump float;\nuniform sampler2D Texture;\nin vec2 Frag_UV;\nin vec4 Frag_Color;\nlayout (location = 0) out vec4 Out_Color;\nvoid main()\n{\n    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n}\n";
   }

   private String getFragmentShaderGlsl410Core() {
      return this.glslVersion
         + "\nin vec2 Frag_UV;\nin vec4 Frag_Color;\nuniform sampler2D Texture;\nlayout (location = 0) out vec4 Out_Color;\nvoid main()\n{\n    Out_Color = Frag_Color * texture(Texture, Frag_UV.st);\n}\n";
   }
}
