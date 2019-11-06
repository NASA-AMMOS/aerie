package gov.nasa.jpl.ammos.mpsa.aerie.merlinmultimissionmodels.data.StateModels;


import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.StateContainer;
import gov.nasa.jpl.ammos.mpsa.aerie.merlinsdk.states.interfaces.State;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The adapter should replace these states, and their associated values, with the ones they require
 */
public class OnboardDataModelStates implements StateContainer {

    public final InstrumentModel instrument_a_data_rate = new InstrumentModel("instrument 1", 0.0);
    public final InstrumentModel instrument_b_data_rate = new InstrumentModel("instrument 2", 0.0);
    public final InstrumentModel instrument_c_data_rate = new InstrumentModel("instrument 3", 0.0);
    public final InstrumentModel instrument_d_data_rate = new InstrumentModel("instrument 4", 0.0);
    public final InstrumentModel instrument_e_data_rate = new InstrumentModel("instrument 4", 0.0);
    public final InstrumentModel instrument_f_data_rate = new InstrumentModel("instrument 4", 0.0);
    public final InstrumentModel instrument_g_data_rate = new InstrumentModel("instrument 4", 0.0);
    public final BinModel bin_1 = new BinModel("Bin 1", instrument_a_data_rate, instrument_b_data_rate);
    public final BinModel bin_2 = new BinModel("Bin 2", instrument_c_data_rate, instrument_d_data_rate);
    public final BinModel bin_3 = new BinModel("Bin 3", instrument_e_data_rate, instrument_f_data_rate, instrument_g_data_rate);

    private List<State<?>> stateList = List.of(instrument_a_data_rate, instrument_b_data_rate, instrument_c_data_rate, instrument_d_data_rate,
            instrument_e_data_rate, instrument_f_data_rate, instrument_g_data_rate, bin_1, bin_2, bin_3);

    public List<State<?>> getStateList() {
        return stateList;
    }

    public Map<String, InstrumentModel> instrumentNameMap = new HashMap<>();
    public Map<String, BinModel> binNameMap = new HashMap<>();

    public OnboardDataModelStates(){
        for (State x : this.stateList){
            if (x.getClass().getSimpleName().equals("BinModel")){
                this.binNameMap.put(x.getName(), (BinModel) x);
            }
            else if (x.getClass().getSimpleName().equals("InstrumentModel")){
                this.instrumentNameMap.put(x.getName(), (InstrumentModel) x);
            }
        }
    }

    public List<InstrumentModel> getInstrumentModelList(){
        return new ArrayList<>(instrumentNameMap.values());
    }

    public List<BinModel> getBinModelList(){
        return new ArrayList<>(binNameMap.values());
    }

    public InstrumentModel getInstrumentByName(String name){
        return this.instrumentNameMap.get(name);
    }

    public BinModel getBinByName(String name){
        return this.binNameMap.get(name);
    }
}



