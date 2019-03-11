package gov.nasa.jpl.schedule;

import gov.nasa.jpl.aerie.schemas.Schedule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedule")
public class ScheduleController {

//    @Autowired
//    private ScheduleRepository repository;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Object> getAlgorithms() {

        return ResponseEntity.ok().build();
    }

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<Object> getSchedule(@RequestParam("algorithm") String algorithm,
                                              @RequestParam("planId") String planId) {


        ScheduleContext context;
        switch(algorithm.toLowerCase()) {
            case "greedy forward dispatch":
                context = new ScheduleContext(new GreedyForwardDispatch(), planId);
                break; // break is optional

            default:
                context = new ScheduleContext(new GreedyForwardDispatch(), planId);
                break;
        }

        Schedule schedule = context.execute();

        return ResponseEntity.ok().build();
    }

}