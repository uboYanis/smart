package controller.Visualiser;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import boiteModale.BoiteModale;
import interfaceModel.Imodel;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import model.Model;
import model.Releve;
import utilitaireController.Graph;
import utilitaireController.UtilitaireController;

/**
 * cette class permet de visualiser l'�volution des releve sur une dur�e d'une
 * journ�e dans un graphe
 * 
 * @author REMILA Yanis
 * @version 1.2
 *
 */
public class VisualiserJourController implements Initializable {
	ObservableList<XYChart.Series<String, Number>> obsList = FXCollections.observableArrayList();
	final CategoryAxis xAxis = new CategoryAxis();
	final NumberAxis yAxis = new NumberAxis();
	@FXML
	private LineChart<String, Number> chartJour = new LineChart<>(xAxis, yAxis, obsList);

	@FXML
	private ChoiceBox<String> Station, parametres;
	@FXML
	private Label messageProgression;
	@FXML
	private ProgressBar progressBar;
	@FXML
	private CheckBox check;
	@FXML
	private Button btnJour;

	@FXML
	private DatePicker Date;
	XYChart.Series<String, Number> series = new XYChart.Series<>();
	UtilitaireController initialiserVue = new UtilitaireController();
	BoiteModale boiteModale = new BoiteModale();
	LocalDate dateLocal = LocalDate.now();
	Imodel model = new Model();
	Thread monThread;

	/**
	 * cette methode permet lanc� une tache dans un thread et cette tache a pour
	 * role de r�cup�rer la liste de releve d'une journ�e et d'afficher une
	 * series(courbe) dans un graph et suivre l'�volution du chargement des
	 * donn�es sur un progresse bar
	 * 
	 * @param stationChercher
	 *            : station chercher
	 * @param anneeChercher
	 *            : annee charcher
	 * @param moisChercher
	 *            : mois chercher
	 * @param jourChercher
	 *            : jour chercher
	 * @param parametre
	 *            : parametre voulue
	 */
	private void tacheDonneeJour(int stationChercher, int anneeChercher, int moisChercher, int jourChercher,
			int parametre) {
		chartJour.getData().clear();
		Task<ObservableList<Releve>> tache = new Task<ObservableList<Releve>>() {
			@Override
			protected ObservableList<Releve> call() throws Exception {
				try {
					List<Releve> listDonneeJour = model.donneeJour(stationChercher, anneeChercher, moisChercher,
							jourChercher);
					Platform.runLater(new Runnable() {
						@Override
						public void run() {

							if (parametre == 1) {
								series = Graph.createSerie(Graph.converteToCel(listDonneeJour), parametre, 2, "Jour");
							} else {
								series = Graph.createSerie(Graph.converteToKel(listDonneeJour), parametre, 2, "Jour");
							}
							chartJour.getData().add(series);
						}
					});
					updateProgress(1, 1);
					updateMessage("chargement terminer");
				} catch (Exception e) {
					updateMessage(e.getMessage());
					updateProgress(0, 0);
					boiteModale.information("erreur", e.getMessage());
				}
				;
				return null;
			}
		};
		initialiserVue.lancerThread(monThread, tache, messageProgression, progressBar);
	}

	/**
	 * permet de r�cuperer les valeurs des champs s�lectionner dans l'IHM et les
	 * passer en parametres a la methode visualiserJour lors du clic sur le btn
	 * visualiser
	 * 
	 * @param event
	 *            :event
	 */
	@FXML
	void visualiserJour(ActionEvent event) {
		LocalDate dateObtenue = Date.getValue();
		int stationhercher = model.getidMapStation(Station.getValue());
		int anneeObtenue = dateObtenue.getYear();
		int moisObtenue = dateObtenue.getMonthValue();
		int jourObtenue = dateObtenue.getDayOfMonth();
		int parametreObtenue = parametres.getSelectionModel().getSelectedIndex() + 1;
		tacheDonneeJour(stationhercher, anneeObtenue, moisObtenue, jourObtenue, parametreObtenue);
	}

	/**
	 * permet ce cr�e une relation maitre esclave entre le btn visualiser et les
	 * autres champs de l'IHM tel que le bntVisualiser reste d�sactiver tant que
	 * tout les champs ne soit pas remplit et que le checkBox ne soit pas
	 * selectionner pour dire que les listener sont d�sactiver
	 */
	public void disableBntVisualiser() {
		BooleanBinding bindAfficheMois = new BooleanBinding() {
			{
				super.bind(Station.valueProperty(), Date.valueProperty(), parametres.valueProperty(),
						check.selectedProperty());
			}

			@Override
			protected boolean computeValue() {
				if (Station.getValue() != null && Date.getValue() != null && parametres.getValue() != null
						&& check.isSelected()) {
					return false;
				}
				return true;
			}
		};
		btnJour.disableProperty().bind(bindAfficheMois);
	}

	/**
	 * v�rifie si tout les champs que l'IHM son non vide
	 * 
	 * @return false si tout les champs non vide sinon true
	 */
	private Boolean isChoiceBoxEmpty() {
		if (Station.getValue() != null && Date.getValue() != null && parametres.getValue() != null
				&& !check.isSelected()) {
			return false;
		}
		return true;
	}

	/**
	 * cette methode notifie le checkBox station de sort que lorsque il change
	 * de valeur si tout les champs sont rempli la methode tacheDonneeJour sera
	 * appel� avec la nouvelle valeur du champ
	 */
	private void obsChoiceBoxStation() {
		Station.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				LocalDate dateObtenue = Date.getValue();
				if (!isChoiceBoxEmpty()) {
					tacheDonneeJour(model.getidMapStation(newValue), dateObtenue.getYear(), dateObtenue.getMonthValue(),
							dateObtenue.getDayOfMonth(), initialiserVue.SelectedIndexParametre(parametres.getValue()));
				}

			}
		});
	}

	/**
	 * cette methode notifie le DatePiker Date de sort que lorsque il change de
	 * valeur si tout les champs sont rempli la methode tacheDonneeJour sera
	 * appel� avec la nouvelle valeur du champ
	 */
	private void obsDatePiker() {
		Date.valueProperty().addListener(new ChangeListener<LocalDate>() {

			@Override
			public void changed(ObservableValue<? extends LocalDate> observable, LocalDate oldValue,
					LocalDate newValue) {
				if (!isChoiceBoxEmpty()) {
					tacheDonneeJour(model.getidMapStation(Station.getValue()), newValue.getYear(),
							newValue.getMonthValue(), newValue.getDayOfMonth(),
							initialiserVue.SelectedIndexParametre(parametres.getValue()));
				}

			}
		});
	}

	/**
	 * cette methode notifie le checkBox parametres de sort que lorsque il
	 * change de valeur si tout les champs sont rempli la methode
	 * tacheDonneeJour sera appel� avec la nouvelle valeur du champ
	 */
	private void obsChoiceBoxParametre() {
		parametres.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {

			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				LocalDate dateObtenue = Date.getValue();
				if (!isChoiceBoxEmpty()) {
					tacheDonneeJour(model.getidMapStation(Station.getValue()), dateObtenue.getYear(),
							dateObtenue.getMonthValue(), dateObtenue.getDayOfMonth(),
							initialiserVue.SelectedIndexParametre(newValue));
				}

			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javafx.fxml.Initializable#initialize(java.net.URL,
	 * java.util.ResourceBundle)
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {

		initialiserVue.initListeStation(Station);
		initialiserVue.initListeParametre(parametres);
		initialiserVue.controlleDate(Date);
		initialiserVue.checkBox(check);
		disableBntVisualiser();
		check.setText("Listener\nActiver");
		obsDatePiker();
		obsChoiceBoxStation();
		obsChoiceBoxParametre();
	}

}
