package dev.taras.hamkalo.plagdet.classification.code;

import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.jetbrains.annotations.NotNull;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.impl.LossBinaryXENT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CodeTextClassifierTraining {

  private static final int EPOCHS = 300;

  private static final double LEARNING_RATE = 0.021;

  private static final int NUM_CLASSES = 2;

  public static void main(String[] args) throws Exception {
    trainAndStore();
//    trainAndEval();
  }

  private static void trainAndStore() throws IOException {

    DataSet allData = createDataset("/home/taras-hamkalo/bin/code-classification/classified_lines.csv");
    allData.shuffle();
    MultiLayerConfiguration conf = buildModel();

    MultiLayerNetwork model = new MultiLayerNetwork(conf);
    model.init();
    model.setListeners(new ScoreIterationListener(10));

    for (int epoch = 0; epoch < EPOCHS; epoch++) {
      model.fit(allData);
    }

    File file = new File("code-classifier.zip");
    ModelSerializer.writeModel(model, file, false);
  }

  private static void trainAndEval() throws IOException {
    DataSet allData = createDataset("/home/taras-hamkalo/bin/code-classification/classified_lines.csv");
    allData.shuffle();

    SplitTestAndTrain split = allData.splitTestAndTrain(0.6);
    DataSet trainData = split.getTrain();
    DataSet testData = split.getTest();
    MultiLayerConfiguration conf = buildModel();

    MultiLayerNetwork model = new MultiLayerNetwork(conf);
    model.init();
    model.setListeners(new ScoreIterationListener(10));

    for (int epoch = 0; epoch < EPOCHS; epoch++) {
      model.fit(trainData);
      System.out.println("Epoch " + epoch);
    }

    INDArray output = model.output(testData.getFeatures());
    Evaluation eval = new Evaluation(2);
    eval.eval(testData.getLabels(), output);
    System.out.println(eval.stats());
  }

  private static MultiLayerConfiguration buildModel() {
    return new NeuralNetConfiguration.Builder()
      .seed(42)
      .updater(new Adam(LEARNING_RATE))
      .list()
      .layer(new DenseLayer.Builder().nIn(FeatureExtractor.FEATURE_VECTOR_SIZE).nOut(32 / 2)
        .activation(Activation.RELU).build())
      .layer(new OutputLayer.Builder()
        .lossFunction(new LossBinaryXENT())
        .activation(Activation.SIGMOID).nIn(32 / 2).nOut(2).build())
      .build();
  }

  private static @NotNull DataSet createDataset(String path) {
    List<TrainingExample> trainingData =
      loadTrainingData(path);

    INDArray featureMatrix = Nd4j.create(trainingData.size(), FeatureExtractor.FEATURE_VECTOR_SIZE);
    INDArray labelMatrix = Nd4j.create(trainingData.size(), NUM_CLASSES);
    for (int i = 0; i < trainingData.size(); i++) {
      TrainingExample ex = trainingData.get(i);
      double[] features = FeatureExtractor.extractFeatures(ex.line());
      featureMatrix.putRow(i, Nd4j.create(features));
      double[] labelVector = new double[NUM_CLASSES];
      labelVector[ex.isCode() ? 1 : 0] = 1.0;
      labelMatrix.putRow(i, Nd4j.create(labelVector));
    }
    return new DataSet(featureMatrix, labelMatrix);
  }

  public static List<TrainingExample> loadTrainingData(String csvPath) {
    List<TrainingExample> examples = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
      String line;
      br.readLine();
      while ((line = br.readLine()) != null) {
        String text = line.substring(0, line.lastIndexOf(","));
        String label = line.substring(line.lastIndexOf(",") + 1);
        if (text.isBlank()) {
          continue;
        }
        if (text.matches("\".*\"")) {
          text = text.substring(1, text.length() - 1);
        }

        boolean isCode = label.trim().equals("1");
        examples.add(new TrainingExample(text, isCode));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return examples;
  }

  public record TrainingExample(String line, boolean isCode) {
  }
}
