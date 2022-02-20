/*
 * Web Servlet for converting SVG documents using Apache Batik to PNG images
 * Copyright (C) 2020 Giedrius Deveikis @ LAMA BPO
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package lt.lamabpo.svg;

import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.FontFormatException;
import static java.lang.Math.toIntExact;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author Giedrius Deveikis @ LAMA BPO
 */
@WebServlet(name = "SVG2PNGServlet", urlPatterns = { "/SVG2PNGServlet" })
@MultipartConfig(fileSizeThreshold=1024*1024)
public class SVG2PNGServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String fontsDirectory;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        fontsDirectory = getServletContext().getInitParameter("fontsDirectory");
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontsDirectory + "/Arimo-Regular.ttf")));
        } catch (FontFormatException | IOException e) {
            getServletContext().log("Unhandled exception while adding font from file", e);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String test = null, dStr;
        int w = 0, h = 0;
        String svg;
        if (request.getHeader("content-type").startsWith("multipart/form-data")) {
            svg = partToString(request.getPart("svg"), null, "UTF-8");
            test = partToString(request.getPart("test"), null, "UTF-8");
            if (test == null || test.length() == 0) {
                w = Integer.parseInt(partToString(request.getPart("w"), "0", "UTF-8"));
                h = Integer.parseInt(partToString(request.getPart("h"), "0", "UTF-8"));
            }
        } else {
            svg = request.getParameter("svg");
            test = request.getParameter("test");
            if (test == null || test.length() == 0) {
                w = Integer.parseInt(request.getParameter("w"));
                h = Integer.parseInt(request.getParameter("h"));
            }
        }
        try (ServletOutputStream sos = response.getOutputStream();
                ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            TranscoderInput ti = new TranscoderInput(new StringReader(svg));
            TranscoderOutput to = new TranscoderOutput(os);
            PNGTranscoder pngT = new PNGTranscoder();
            if (w > 0)
                pngT.addTranscodingHint(ImageTranscoder.KEY_WIDTH, new Float(w));
            if (h > 0)
                pngT.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, new Float(h));
            pngT.transcode(ti, to);
            os.flush();
            response.setContentType("image/png");
            response.setContentLength(os.size());
            os.writeTo(sos);
            sos.flush();
        } catch (TranscoderException ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * 
     * @param input       one part of multipart/form-data
     * @param charsetName source charset of binary part data
     * @return
     * @throws IOException
     */
    public static String partToString(final Part input, final String defaultVal, final String charsetName)
            throws IOException {
        try (InputStream inputStream = input.getInputStream()) {
            byte[] buffer = new byte[toIntExact(input.getSize())];
            inputStream.read(buffer);
            return new String(buffer,charsetName);
        } catch (NullPointerException e) {
            return defaultVal;
        }
    }
}
