/*
public class DEGenericTautomerCreator implements Runnable {
    public DEGenericTautomerCreator(Frame owner, CompoundTableModel tableModel) {
	public void create(int structureColumn) {

		Thread t = new Thread(this, "DEGenericTautomerCreator");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
		}

	public void run() {
		runAnalysis();

		mProgressDialog.stopProgress();
	    mProgressDialog.close(null);
		}

	private void runAnalysis() {
		mProgressDialog.startProgress("Creating generic tautomers...", 0, mTableModel.getTotalRowCount());

		for (int row=0; row<mTableModel.getTotalRowCount(); row++) {
			if (mProgressDialog.threadMustDie())
				break;

			mProgressDialog.updateProgress(row+1);

			StereoMolecule mol = mTableModel.getChemicalStructure(mTableModel.getTotalRecord(row), mStructureColumn, CompoundTableModel.ATOM_COLOR_MODE_NONE, molContainer);
			if (mol != null) {
    			    Canonizer canonizer = new Canonizer(tautomer, Canonizer.ENCODE_ATOM_CUSTOM_LABELS);
    				    mTableModel.setTotalValueAt(canonizer.getEncodedCoordinates(), row, firstColumn+2);
			}

	}