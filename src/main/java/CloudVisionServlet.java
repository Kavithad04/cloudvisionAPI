import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@MultipartConfig
@WebServlet(name = "CloudVisionServlet")
public class CloudVisionServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*Writer writer = response.getWriter();*/
        Part requestPart = request.getPart("fileToUpload");
        InputStream stream = requestPart.getInputStream();
        /*String projectId = "eco-cyclist-293107";
        String bucketName = "mypics90";
        String[] headerTok = requestPart.getHeader("content-disposition").split(";");
        /*String fileName = "no-name";
        for(String tok: headerTok){
            if(tok.contains("filename")){
                String name = tok.split("=")[1];
                fileName = name.substring(1, name.length()-1);
            }
        }*/
        String filePath = "temp";
        File imageFile = new File(filePath);
        OutputStream os = new FileOutputStream(imageFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(os);
        while(stream.available()>0) {
            bufferedOutputStream.write(stream.read());
        }
        bufferedOutputStream.flush();
        bufferedOutputStream.close();
        os.flush();os.close();
       /* Storage storage =  StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
        storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));*/
        List<AnnotateImageResponse> labelResponses = generateLabel(filePath);
        for (AnnotateImageResponse res : labelResponses) {
            if (res.hasError()) {
                 response.getWriter().println("Error: %s%n" + res.getError().getMessage());
            }

            for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
               Map<Descriptors.FieldDescriptor, Object> fields = annotation.getAllFields();
               for(Descriptors.FieldDescriptor fd: fields.keySet()){
                   if(!fd.getName().contains("mid") && !fd.getName().contains("topicality"))
                   response.getWriter().println(fd.getJsonName() + ":" + fields.get(fd).toString());
               }
            }
        }
        imageFile.deleteOnExit();
    }

    private List<AnnotateImageResponse> generateLabel(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();


        System.out.println(System.getenv("GOOGLE_APPLICATION_CREDENTIALS"));//give your path name

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try {
            ImageAnnotatorClient client = ImageAnnotatorClient.create();
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            return responses;

        } catch (Exception e){
            System.out.println(e);
        }
        return null;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
