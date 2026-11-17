package controller;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import data.ProdukRepository;
import model.Produk;

public class KatalogController {

    @FXML private TextField namaField;
    @FXML private TextField hargaField;
    @FXML private Label fotoPathLabel;
    @FXML private Label pesanLabel;
    @FXML private Label jumlahLabel;
    @FXML private Label formStatusLabel;
    @FXML private TilePane katalogPane;
    @FXML private Button batalEditBtn;
    @FXML private Button simpanBtn;

    private final ProdukRepository repository = new ProdukRepository();
    
    private String selectedFotoUri = "";
    private long editProductId = 0;
    private boolean isEditMode = false;

    @FXML
    private void initialize() {
        try {
            showData();
        } catch (SQLException e) {
            pesanLabel.setText("Gagal membaca database.");
            e.printStackTrace();
        }
    }

    private void showData() throws SQLException {
        ArrayList<Produk> daftar = repository.getAll();
        katalogPane.getChildren().clear();
        for (Produk produk : daftar) {
            VBox kartu = createProductCard(produk);
            katalogPane.getChildren().add(kartu);
        }
        jumlahLabel.setText("Jumlah produk: " + daftar.size());
    }

    @FXML
    private void pilihFotoLokal() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Foto Produk");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(namaField.getScene().getWindow());
        if (file != null) {
            selectedFotoUri = file.toURI().toString(); // Simpan URI lokal
            fotoPathLabel.setText(file.getName());
        }
    }

    private VBox createProductCard(Produk produk) {
        ImageView fotoView = new ImageView();
        fotoView.setFitWidth(170);
        fotoView.setFitHeight(110);
        fotoView.setPreserveRatio(true);
        StackPane bingkai = new StackPane();
        bingkai.setPrefSize(180, 120);
        bingkai.setMinSize(180, 120);
        bingkai.setMaxSize(180, 120);

        Image gambar = null;
        try {
            if (produk.getFoto().startsWith("file:/")) {
                gambar = new Image(produk.getFoto(), 170, 110, true, true);
            } else {
                java.net.URL lokasi = getClass().getResource("/image/" + produk.getFoto());
                if (lokasi != null) {
                    gambar = new Image(lokasi.toExternalForm(), 170, 110, true, true);
                }
            }
        } catch (Exception e) {
            // Abaikan jika error load image
        }

        if (gambar != null && !gambar.isError()) {
            fotoView.setImage(gambar);
            bingkai.getChildren().add(fotoView);
        } else {
            bingkai.getChildren().add(new Label("Gambar tak tersedia"));
        }

        Label namaLabel = new Label(produk.getNama());
        namaLabel.setWrapText(true);
        namaLabel.getStyleClass().add("nama-produk");
        Label hargaLabel = new Label("Rp" + produk.getHarga());
        Label idLabel = new Label("ID: " + produk.getId());
        idLabel.getStyleClass().add("muted");

        Button editBtn = new Button("Edit");
        editBtn.setOnAction(e -> prepareEdit(produk));
        
        Button hapusBtn = new Button("Hapus");
        hapusBtn.setOnAction(e -> confirmDelete(produk));
        
        HBox aksiBox = new HBox(10, editBtn, hapusBtn);

        VBox kartu = new VBox();
        kartu.setSpacing(8);
        kartu.setMinWidth(220);
        kartu.setPrefWidth(220);
        kartu.setMaxWidth(220);
        kartu.getStyleClass().add("kartu-produk");
        kartu.getChildren().addAll(bingkai, namaLabel, hargaLabel, idLabel, aksiBox);
        return kartu;
    }

    @FXML
    private void saveProduct() {
        try {
            String nama = namaField.getText();
            long harga = Long.parseLong(hargaField.getText().trim());
            
            if (selectedFotoUri.isEmpty() && !isEditMode) {
                pesanLabel.setText("Silakan pilih foto produk lokal.");
                return;
            }

            Produk produk = new Produk(isEditMode ? editProductId : 0, nama, harga, selectedFotoUri);

            if (isEditMode) {
                repository.update(produk);
                pesanLabel.setText("Produk berhasil diperbarui.");
            } else {
                repository.add(produk);
                pesanLabel.setText("Produk berhasil ditambahkan.");
            }

            resetForm();
            showData();
        } catch (NumberFormatException e) {
            pesanLabel.setText("Harga harus angka bulat.");
        } catch (IllegalArgumentException | SQLException e) {
            pesanLabel.setText(e.getMessage());
        }
    }

    private void prepareEdit(Produk produk) {
        isEditMode = true;
        editProductId = produk.getId();
        namaField.setText(produk.getNama());
        hargaField.setText(String.valueOf(produk.getHarga()));
        selectedFotoUri = produk.getFoto();
        fotoPathLabel.setText("Foto lama dipakai (pilih baru untuk mengganti)");
        
        formStatusLabel.setText("Mode Edit Produk ID: " + editProductId);
        simpanBtn.setText("Update Produk");
        batalEditBtn.setVisible(true);
        batalEditBtn.setManaged(true);
    }

    private void confirmDelete(Produk produk) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Hapus Produk");
        alert.setHeaderText("Hapus " + produk.getNama() + "?");
        alert.setContentText("Tindakan ini tidak bisa dibatalkan.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                repository.delete(produk.getId());
                pesanLabel.setText("Produk berhasil dihapus.");
                if (isEditMode && editProductId == produk.getId()) {
                    resetForm();
                }
                showData();
            } catch (SQLException e) {
                pesanLabel.setText("Gagal menghapus data.");
            }
        }
    }

    @FXML
    private void resetForm() {
        isEditMode = false;
        editProductId = 0;
        namaField.clear();
        hargaField.clear();
        selectedFotoUri = "";
        fotoPathLabel.setText("Belum ada gambar dipilih");
        formStatusLabel.setText("Tambah Produk Baru");
        simpanBtn.setText("Simpan");
        batalEditBtn.setVisible(false);
        batalEditBtn.setManaged(false);
    }
}