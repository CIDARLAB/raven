package Communication;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.FileUtils;

public class UploadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     * response)
     *
     */
    @Override
    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.write("call POST with multipart form data");
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     * response)
     *
     */
    @SuppressWarnings("unchecked")
    @Override
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IllegalArgumentException(
                    "Request is not multipart, please 'multipart/form-data' enctype for your form.");
        }

        ServletFileUpload uploadHandler = new ServletFileUpload(
                new DiskFileItemFactory());
        PrintWriter writer = response.getWriter();
        response.setContentType("text/plain");
        response.sendRedirect("import.html");
        Cookie[] cookies = request.getCookies();
        String user = "default";
                for(int i =0;i<cookies.length;i++) {
                    if(cookies[i].getName().equals("user")) {
                        user = cookies[i].getValue();
                    }
                }
        try {
            
            List<FileItem> items = uploadHandler.parseRequest(request);
            String uploadFilePath = this.getServletContext().getRealPath("/")+"/data/"+user+"/";
            new File(uploadFilePath).mkdir();
            for (FileItem item : items) {

                File file;
                if (!item.isFormField()) {
                    String fileName = item.getName();
                    if (fileName == "") {
                        System.out.println("You forgot to choose a file.");
                    }
                    if (fileName.lastIndexOf("\\") >= 0) {
                        file = new File(uploadFilePath +"\\"+ fileName.substring(fileName.lastIndexOf("\\")));
                    } else {
                        file = new File(uploadFilePath +"\\"+ fileName.substring(fileName.lastIndexOf("\\") + 1));
                    }
                    item.write(file);
                }

                writer.write("{\"name\":\"" + item.getName() + "\",\"type\":\"" + item.getContentType() + "\",\"size\":\"" + item.getSize() + "\"}");
            }
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            //File not found, no need to panick, or do anything....
        } finally {
            writer.close();

        }

    }
}