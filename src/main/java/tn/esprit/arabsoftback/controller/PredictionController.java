package tn.esprit.arabsoftback.controller;

import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Translator;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;

@RestController
@RequestMapping("/api/cin-validator")
public class PredictionController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);

    private final ResourceLoader resourceLoader;

    @Value("${ai.model.path:classpath:models/swin_cin_classifier.pt}")
    private String modelPath;

    private ZooModel<Image, Classifications> model;

    public PredictionController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() {
        try {
            Path tempModelPath = resolveModelToTempFile();
            Translator<Image, Classifications> translator = ImageClassificationTranslator.builder()
                    .addTransform(new Resize(224))
                    .addTransform(new ToTensor())
                    .addTransform(new Normalize(
                            new float[]{0.485f, 0.456f, 0.406f},
                            new float[]{0.229f, 0.224f, 0.225f}))
                    .optApplySoftmax(true)
                    .optSynset(Arrays.asList("invalid", "valid"))
                    .build();

            Criteria<Image, Classifications> criteria = Criteria.builder()
                    .setTypes(Image.class, Classifications.class)
                    .optModelPath(tempModelPath)
                    .optTranslator(translator)
                    .optEngine("PyTorch")
                    .build();

            this.model = ModelZoo.loadModel(criteria);
            logger.info("Modele SWIN charge avec succes depuis {}", modelPath);
        } catch (Exception e) {
            logger.error("Echec du chargement du modele SWIN : {}", e.getMessage(), e);
            this.model = null;
        }
    }

    private Path resolveModelToTempFile() throws Exception {
        if (StringUtils.hasText(modelPath) && !modelPath.startsWith("classpath:")) {
            Path filePath = toFilesystemPath(modelPath);
            if (!Files.isRegularFile(filePath)) {
                throw new java.io.FileNotFoundException("Fichier modele introuvable : " + filePath);
            }
            return copyToTemp(filePath.getFileName().toString(), Files.newInputStream(filePath));
        }

        if ("classpath:models/".equals(modelPath) || modelPath.endsWith("/models/")) {
            Resource resourceDir = resourceLoader.getResource("classpath:models/");
            File[] modelFiles = listModelFiles(resourceDir);
            if (modelFiles == null || modelFiles.length == 0) {
                throw new java.io.FileNotFoundException("Aucun modele swin_cin_classifier*.pt dans classpath:models/");
            }
            File latestModel = Arrays.stream(modelFiles)
                    .max(Comparator.comparingLong(File::lastModified))
                    .orElse(modelFiles[0]);
            try (InputStream is = new FileInputStream(latestModel)) {
                return copyToTemp(latestModel.getName(), is);
            }
        }

        String classpathLocation = modelPath.startsWith("classpath:")
                ? modelPath
                : "classpath:" + modelPath;
        Resource resource = resourceLoader.getResource(classpathLocation);
        if (!resource.exists()) {
            throw new java.io.FileNotFoundException("Modele introuvable : " + classpathLocation);
        }
        String fileName = classpathLocation.substring(classpathLocation.lastIndexOf('/') + 1);
        try (InputStream is = resource.getInputStream()) {
            return copyToTemp(fileName, is);
        }
    }

    private File[] listModelFiles(Resource resourceDir) throws Exception {
        try {
            File dir = resourceDir.getFile();
            File[] files = dir.listFiles((d, name) -> name.startsWith("swin_cin_classifier") && name.endsWith(".pt"));
            if (files != null && files.length > 0) {
                return files;
            }
        } catch (Exception ignored) {
            // En JAR Docker : getFile() echoue, on utilise le chemin explicite
        }
        return null;
    }

    private Path toFilesystemPath(String location) {
        if (location.startsWith("file:")) {
            return Paths.get(URI.create(location));
        }
        return Paths.get(location);
    }

    private Path copyToTemp(String name, InputStream is) throws Exception {
        String suffix = name.endsWith(".pt") ? ".pt" : ".pt";
        Path tempModelPath = Files.createTempFile("swin_cin_classifier", suffix);
        Files.copy(is, tempModelPath, StandardCopyOption.REPLACE_EXISTING);
        return tempModelPath;
    }

    @PostMapping("/verify")
    public ResponseEntity<PredictionResponse> verifyCin(@RequestParam("cin_image") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new PredictionResponse("invalid", 0.0, false, false));
        }

        if (model == null) {
            logger.warn("Validation SWIN refusee : modele non charge");
            return ResponseEntity.status(503).body(new PredictionResponse("error", 0.0, false, false));
        }

        try (InputStream is = file.getInputStream()) {
            Image image = ImageFactory.getInstance().fromInputStream(is);
            try (Predictor<Image, Classifications> predictor = model.newPredictor()) {
                Classifications prediction = predictor.predict(image);
                Classifications.Classification best = prediction.best();

                String originalClass = best.getClassName();
                double confidence = best.getProbability() * 100;
                boolean isValid = originalClass.equals("valid");
                boolean adjusted = false;

                if (confidence < 80.0) {
                    isValid = false;
                    originalClass = "invalid";
                    adjusted = true;
                }

                PredictionResponse response = new PredictionResponse(originalClass, confidence, isValid, adjusted);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la prediction SWIN : {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new PredictionResponse("error", 0.0, false, false));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<java.util.Map<String, Object>> health() {
        return ResponseEntity.ok(java.util.Map.of(
                "loaded", model != null,
                "modelPath", modelPath
        ));
    }

    public static class PredictionResponse {
        private String status;
        private double confidence;
        private boolean valid;
        private boolean adjusted;

        public PredictionResponse(String status, double confidence, boolean valid, boolean adjusted) {
            this.status = status;
            this.confidence = confidence;
            this.valid = valid;
            this.adjusted = adjusted;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public boolean isAdjusted() { return adjusted; }
        public void setAdjusted(boolean adjusted) { this.adjusted = adjusted; }
    }
}
