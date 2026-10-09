/**
 * <PRE>
 * 
 * Copyright Tony Bringarder 1998, 2025 <A href="http://bringardner.com/tony">Tony Bringardner</A>
 * 
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       <A href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</A>
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  </PRE>
 *   
 *   
 *	@author Tony Bringardner   
 *
 *
 * ~version~V000.01.26-V000.00.01-V000.00.00-
 */
package us.bringardner.parley.files.viewer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileTransferable implements Transferable {
	private static DataFlavor [] SUPPORTED = {DataFlavor.javaFileListFlavor};
	
	private java.util.List<File> data;
	private boolean backup;
	
	public boolean isBackup() {
		return backup;
	}

	public void setBackup(boolean backup) {
		this.backup = backup;
	}

	public FileTransferable(List<File> files, boolean backup) {
		data = files;
		this.backup = backup;
	}

	
	public Object getTransferData(DataFlavor arg0)	throws UnsupportedFlavorException, IOException {	
		if( isDataFlavorSupported(arg0)) {
			return data;
		} else {
			return null;
		}
	}

	
	public DataFlavor[] getTransferDataFlavors() {
		return SUPPORTED;
	}

	
	public boolean isDataFlavorSupported(DataFlavor arg0) {
		if (DataFlavor.javaFileListFlavor.equals(arg0)) {
            return true;
        } 
        return false;
	}

}
