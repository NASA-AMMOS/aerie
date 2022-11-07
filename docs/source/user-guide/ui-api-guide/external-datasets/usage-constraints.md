# Using External Profiles in Constraints

After the external dataset is uploaded, constraints can access the included profiles just as if they were simulated profiles.
The key difference is that since external datasets are associated with plans and simulated datasets are associated with models, the constraint must be associated with the
same plan to access the external profile.

External profiles can contain gaps, and currently simulated profiles cannot. Gaps in profile transformations will be preserved; i.e. comparing the equality of two profiles with gaps will include the gaps, because the result of the operation is unknown. This means that windows can also have gaps, as windows are essentially boolean profiles. Ultimately the gaps are reflected in the constraint's violations as a warning, meaning the constraint _might_ be violated because the relevant profiles had unknown values.

Gaps can be removed at any step in the constraint code by calling the `.assignGaps(<value>)` method, which replaces all gaps in the profile with the given value. This can be useful on the resulting windows object that gets turned in to a constraint:

```ts
export default (): Constraint => {
    let result = /* compute your constraint windows */;
    
    // This says that gaps are nominal (non-violating).
    return result.assignGaps(true);
    
    // OR
    
    // This says that gaps are violations.
    return result.assignGaps(false);
    
    // OR
  
    // This will display gaps as warnings.
    return result;
}
```
