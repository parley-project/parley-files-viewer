/**
 * Copyright Tony Bringardner 1998, 2026 <a href="http://bringardner.com/tony">Tony Bringardner</a>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package us.bringardner.parley.files.viewer.fx;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.net.ssl.TrustManager;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import us.bringardner.parley.core.SecureBaseObject;
import us.bringardner.parley.net.client.DynamicTrustManager;
import us.bringardner.parley.net.client.DynamicTrustManager.CertificateValidator;

/**
 * Asks the user about a server certificate nothing else trusts: the JavaFX replacement for
 * parley-net's Swing {@code VisualCertificateValidator}. {@link DynamicTrustManager} calls it
 * during a TLS handshake, on a connection thread; the dialog is shown on the JavaFX thread and
 * the handshake waits for the answer.
 * <p>
 * The dialog shows the host, who the certificate is for and who signed it, when it's good, its
 * SHA-256 and SHA-1 fingerprints, and warnings for what's wrong with it (expired, self-signed,
 * for another host). Rejecting is the default and what closing the window means. An approval is
 * remembered by the trust manager for that host and certificate only.
 */
public final class FxCertificateValidator implements CertificateValidator {

	private final Function<CertificateInfo, ManageAs> prompt;
	// answers given this session, so several connections to one server ask once
	private final Map<String,ManageAs> answers = new ConcurrentHashMap<>();
	private final Object askLock = new Object();
	private volatile Window owner;

	public FxCertificateValidator() {
		this(null);
	}

	/** @param prompt asks the user, on any thread; null to show the dialog */
	FxCertificateValidator(Function<CertificateInfo, ManageAs> prompt) {
		this.prompt = prompt != null ? prompt : this::askOnFxThread;
	}

	/**
	 * Makes connections ask through a dialog about certificates the JVM's trust store doesn't
	 * trust, as parley-files-viewer does with its Swing dialog.
	 *
	 * @return the validator, so a window can be named as the dialog's owner
	 */
	public static FxCertificateValidator install() {
		FxCertificateValidator validator = new FxCertificateValidator();
		TrustManager[] managers = { new DynamicTrustManager(null, validator) };
		SecureBaseObject.setDefaultTrustManagers(managers);
		DynamicTrustManager.setDefaultValidator(validator);
		return validator;
	}

	/** The window the dialog belongs to (it's centered over it); null for none. */
	public void setOwner(Window owner) {
		this.owner = owner;
	}

	@Override
	public ManageAs validate(X509Certificate cert) {
		return validate(cert, null);
	}

	@Override
	public ManageAs validate(X509Certificate cert, String host) {
		CertificateInfo info = new CertificateInfo(cert, host, new Date());
		String key = (host == null ? "" : host.toLowerCase())+"|"+info.getSha256();
		// one question at a time: a second connection to the same server then finds the answer
		synchronized (askLock) {
			ManageAs known = answers.get(key);
			if( known != null ) {
				return known;
			}
			ManageAs answer;
			try {
				answer = prompt.apply(info);
			} catch (RuntimeException e) {
				answer = ManageAs.REJECT;
			}
			if( answer == null ) {
				answer = ManageAs.REJECT;
			}
			if( answer != ManageAs.REJECT ) {
				answers.put(key, answer);
			}
			return answer;
		}
	}

	// ---------------------------------------------------------------- the dialog

	private ManageAs askOnFxThread(CertificateInfo info) {
		if( Platform.isFxApplicationThread()) {
			return show(info);
		}
		CompletableFuture<ManageAs> answer = new CompletableFuture<>();
		try {
			Platform.runLater(()->{
				try {
					answer.complete(show(info));
				} catch (RuntimeException e) {
					answer.complete(ManageAs.REJECT);
				}
			});
		} catch (IllegalStateException toolkitNotRunning) {
			return ManageAs.REJECT;
		}
		try {
			return answer.get();
		} catch (Exception e) {
			return ManageAs.REJECT;
		}
	}

	private ManageAs show(CertificateInfo info) {
		ButtonType reject = new ButtonType("Reject", ButtonData.CANCEL_CLOSE);
		ButtonType once = new ButtonType("Accept This Time", ButtonData.OK_DONE);
		ButtonType always = new ButtonType("Always Accept", ButtonData.YES);
		Dialog<ManageAs> dialog = new Dialog<>();
		dialog.setTitle("Untrusted Certificate");
		dialog.setHeaderText("The server "+(info.getHost() == null ? "" : info.getHost()+" ")
				+"presented a certificate that isn't trusted.");
		Window w = owner;
		if( w != null && w.isShowing()) {
			dialog.initOwner(w);
		}
		dialog.setResizable(true);
		FxLook.apply(dialog);

		VBox content = new VBox(10);
		content.setPadding(new Insets(4));
		List<String> warnings = info.warnings();
		if( !warnings.isEmpty()) {
			Label warn = new Label(String.join("\n", warnings));
			warn.setWrapText(true);
			warn.setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
			content.getChildren().add(warn);
		}
		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(6);
		int row = 0;
		row = addRow(grid, row, "Server", info.getHost() == null ? "unknown" : info.getHost());
		row = addRow(grid, row, "Issued to", info.getSubject());
		row = addRow(grid, row, "Issued by", info.getIssuer());
		row = addRow(grid, row, "Valid from", String.valueOf(info.getNotBefore()));
		row = addRow(grid, row, "Valid to", String.valueOf(info.getNotAfter()));
		row = addRow(grid, row, "Serial number", info.getSerial());
		row = addRow(grid, row, "SHA-256", info.getSha256());
		row = addRow(grid, row, "SHA-1", info.getSha1());
		content.getChildren().add(grid);
		Label advice = new Label("Accept it only if you trust this server. If you can, compare the fingerprint "
				+"with the one the server's administrator gave you. \"Always Accept\" remembers it for this server.");
		advice.setWrapText(true);
		advice.setPrefWidth(560);
		content.getChildren().add(advice);

		dialog.getDialogPane().setContent(content);
		dialog.getDialogPane().getButtonTypes().addAll(reject, once, always);
		// the safe answer is the one Enter gives
		Button rejectButton = (Button) dialog.getDialogPane().lookupButton(reject);
		rejectButton.setDefaultButton(true);
		((Button) dialog.getDialogPane().lookupButton(once)).setDefaultButton(false);
		((Button) dialog.getDialogPane().lookupButton(always)).setDefaultButton(false);
		dialog.setResultConverter(b->b == once ? ManageAs.ACCEPT_ONCE : b == always ? ManageAs.ACCEPT_ALWAYS : ManageAs.REJECT);
		return dialog.showAndWait().orElse(ManageAs.REJECT);
	}

	private static int addRow(GridPane grid, int row, String label, String value) {
		Label l = new Label(label+":");
		l.setStyle("-fx-font-weight: bold;");
		// selectable, so a fingerprint can be copied to compare
		TextArea v = new TextArea(value);
		v.setEditable(false);
		v.setWrapText(true);
		v.setPrefRowCount(value.length() > 70 ? 2 : 1);
		v.setPrefColumnCount(48);
		v.setMinHeight(Region_USE_PREF);
		GridPane.setHgrow(v, Priority.ALWAYS);
		grid.add(l, 0, row);
		grid.add(v, 1, row);
		return row+1;
	}

	private static final double Region_USE_PREF = javafx.scene.layout.Region.USE_PREF_SIZE;
}
