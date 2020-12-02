package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    @FXML
    private Button run;
    @FXML
    private TextArea textArea;
    @FXML
    private VBox output;
    @FXML
    private VBox controlPanel;
    @FXML
    private Shape isRunning;
    @FXML
    private Label exitCode;

    private File script = new File(System.getProperty("user.dir") + File.separator + "Script" + File.separator + "script.kts");

    //basic setup
    public void initialize() throws FileNotFoundException {
        Image img = new Image(new FileInputStream(System.getProperty("user.dir") + File.separator + "assets" + File.separator + "img" + File.separator + "play.png"));
        ImageView view = new ImageView(img);
        view.setFitHeight(32);
        view.setFitWidth(32);
        run.setPrefHeight(32);
        run.setPrefWidth(32);
        run.setGraphic(view);
        view.setPreserveRatio(true);
        controlPanel.setStyle("-fx-background-color: #dddddd;");
    }

    //on play click
    public void runCode(ActionEvent actionEvent){
        isRunning.setFill(Color.GREEN);
        clearFile();
        if(!script.exists()){
            try{
                if(script.createNewFile()){
                    System.out.println("Script was created");
                    writeIntoFile(script, textArea.getText());
                }
            }catch (Exception e){
                System.out.println(e.getMessage());
            }
        }else {
            writeIntoFile(script, textArea.getText());
        }

        //creating runtime
        Runtime runtime = Runtime.getRuntime();
        Process p = null;
        output.getChildren().removeAll(output.getChildren());
        try {
            //starting the command
            p = runtime.exec("cmd.exe /c kotlinc -script " + script.getAbsolutePath());

            InputStream processOutput = p.getInputStream();
            Reader r = new InputStreamReader(processOutput);
            BufferedReader br = new BufferedReader(r);
            String line;
            //getting output (if everything was compiled fine)
            while ((line = br.readLine()) != null) {
                output.getChildren().add(new Label(line));
            }

            InputStream processError = p.getErrorStream();
            Reader rError = new InputStreamReader(processError);
            BufferedReader brError = new BufferedReader(rError);
            String lineError;
            //getting errors
            while ((lineError = brError.readLine()) != null) {
                if (lineError.matches(".*script[.]{1}kts:[0-9]+:[0-9]+.*")) {
                    output.getChildren().addAll(localizer(lineError));
                }else{
                    Label label = new Label();
                    label.setTextFill(Color.RED);
                    label.setText(lineError);
                    output.getChildren().add(label);
                }

            }

            p.waitFor();
            int code = p.exitValue();
            //setting the output code
            exitCode.setText("Exit Code: " + Integer.toString(code));


        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
        finally{
            if (p != null)
                p.destroy();
        }
        isRunning.setFill(Color.WHITE);
    }

    //writing the content from the textArea to a file
    public void writeIntoFile(File file, String text){
        try {
            FileWriter myWriter = new FileWriter(file.getPath());
            myWriter.write(text);
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred while writing to the File");
            e.printStackTrace();
        }
    }

    //clearing the file
    public void clearFile(){
        if (script.exists()){
            writeIntoFile(script, "");
            System.out.println("Script was cleared");
        }
    }


    //underline the errors with localization
    public HBox localizer(String line){
        HBox hBox = new HBox();
        Pattern pattern = Pattern.compile("script[.]{1}kts:[0-9]+:[0-9]+");
        Matcher matcher = pattern.matcher(line);
        Text before =  new Text();
        Text after = new Text();
        Text location = new Text();
        location.setFill(Color.BLUE);
        after.setFill(Color.RED);
        before.setFill(Color.RED);
        location.setUnderline(true);
        while(matcher.find()){
            before.setText(line.substring(0,matcher.start()));
            location.setText(line.substring(matcher.start(), matcher.end()));
            after.setText(line.substring(matcher.end(),line.length()));
            location.setOnMouseClicked(click -> {
                if (click.getButton() == MouseButton.PRIMARY) {
                    //spliting the localization to get the line and position in the line
                    String[] splited = location.getText().split(":");
                    setCursor(Integer.parseInt(splited[1]), Integer.parseInt(splited[2]));
                }
            });
            hBox.getChildren().addAll(before, location, after);
        }
        return hBox;
    }

    //make the error localization clickable
    public void setCursor(int line, int position){
        textArea.requestFocus();
        int pos = 0;
        String[] withoutEnter = textArea.getText().split("\n");
        if(line == 1){
            textArea.positionCaret(position-1);
        }else{
            for(int i = 1; i<line; i++){
                pos += withoutEnter[i-1].length()+1;
            }
            textArea.positionCaret(pos+position-1);
        }
    }

}