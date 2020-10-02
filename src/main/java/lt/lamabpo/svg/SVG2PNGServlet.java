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
import java.io.IOException;
import java.io.StringReader;
import java.awt.GraphicsEnvironment;
import java.awt.Font;
import java.awt.FontFormatException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 *
 * @author Giedrius Deveikis @ LAMA BPO
 */
@WebServlet(name = "SVG2PNGServlet", urlPatterns = { "/SVG2PNGServlet" })
@MultipartConfig
public class SVG2PNGServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private String fontsDirectory;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        fontsDirectory = getServletContext().getInitParameter("fontsDirectory");
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, new File(fontsDirectory + "/Arimo-Regular.ttf")));
        } catch (FontFormatException|IOException e) {
            getServletContext().log("Unhandled exception while adding font from file",e);
        }
    }
    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String test = null,dStr;
            int w=0,h=0;
            String svg;
            if(request.getHeader("content-type").startsWith("multipart/form-data")){
                svg = IOUtils.toString(request.getPart("svg").getInputStream(), "UTF-8");
                try{
                    test = IOUtils.toString(request.getPart("test").getInputStream(), "UTF-8");
                }catch(IOException | NullPointerException e){
                    try{
                        dStr = IOUtils.toString(request.getPart("w").getInputStream(), "UTF-8");
                        w = Integer.parseInt(dStr);
                    }catch(NullPointerException n){}
                    try{
                        dStr = IOUtils.toString(request.getPart("h").getInputStream(), "UTF-8");
                        h = Integer.parseInt(dStr);
                    }catch(NullPointerException n){}
                }
            }else{
                svg = request.getParameter("svg");
                test = request.getParameter("test");
                if(test==null||test.length()==0){
                    w = Integer.parseInt(request.getParameter("w"));
                    h = Integer.parseInt(request.getParameter("h"));
                }
            }
            TranscoderInput ti = new TranscoderInput(new StringReader(svg));
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            TranscoderOutput to = new TranscoderOutput(os);
            PNGTranscoder pngT = new PNGTranscoder();
            if(w>0)
                pngT.addTranscodingHint(ImageTranscoder.KEY_WIDTH,new Float(w));
            if(h>0)
                pngT.addTranscodingHint(ImageTranscoder.KEY_HEIGHT,new Float(h));
            pngT.transcode(ti, to);
            os.flush();
            response.setContentType("image/png");
            response.setContentLength(os.size());
            os.writeTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (TranscoderException ex) {
            throw new ServletException(ex);
        }
    }
}
