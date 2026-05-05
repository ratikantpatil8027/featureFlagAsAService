//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
void main() {
    //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
    // to see how IntelliJ IDEA suggests fixing it.
    IO.println(String.format("Hello and welcome!"));

    for (int i = 1; i <= 5; i++) {
        //TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
        // for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.
        IO.println("i = " + i);
    }
    /*
    * Requirements
    * feature_flag -> client, name, <attributes>, status, rollout
    * feature_flag_rollout_key ->
    *
    * cache read -> allow x% -> reject 100-x% - save in cache XXXXX
    *
    * key -> rollout attributes
    * <user_id> -> key - hash () - <>

    *
    * user_id - nskjnkja17918n -> num % percentage_derivative -> key
    *
    *
    *
    *
    * CRUD feature flag
    * Create/Update - client, name, <attributes>, formula_string, rollout = 0 -> feature_flag_id
    * Delete is delete feature_flag_id
    *
    * attributes:{user_id:num, region:string} , formula_string:"user_id NOT IN (a,b,c) AND region != ""Canada"
    * formulae parser - score * 0.99 + arithmatic > threshold - yes/no
    *
    * Operation - {input1, input2, operand}
    * operands - AND, OR, IN, ,,,,,
    * Nested set of operations
    *
    *
    * get feature flag for client, name, attributes, feature_flag_id
    * - do validations check for client, name (basic validations for feature_flag_id)
    * - compute rollout check (for all attributes, check whether request attributes fall in rollout bucket)
    * - verify attributes check ()
    *
    * - compute flag value and return
    *
    *
    * rollout feature - feature_flag_id, percentage, <attributes>
    *
    * feature flags
    *
    * Statement -
    *
    * Solid Requirements -
    *
    * Data Flow -
    *
    * Structure -
    * models -  featureflag - client:string, name:string, <attributes>:jsonObject, formula_string:string, rollout:int = 0 -> feature_flag_id:string
    *                       - example for attributes:{user_id:num, region:string} , formula_string:"user_id NOT IN (a,b,c) AND region != ""Canada"
    *           featureflagrolloutkey - feature_flag_id:string, <attributes>:jsonObject, rollout_key:string, threshold:int
    *                                 - example for attributes {user_id:num}, rollout_key = hxdrt1425, threshold = 20
    *
    * service - featureflagservice
    *               - create feature flag method creates a featureflag entry with rollout 0, init featureflagrolloutkey entry
    *                 * accepts {client, name, <attributes>, formula_string, rollout = 0}
    *                 * validates jsonObject and formula using formula parser and json object validator
    *                 * init featureflagrolloutkey with basic hash key and 0 threshold (inactive feature)
    *                 * returns UUID feature_flag_id
    *               - update feature flag method updates featureflag entry
    *                 * validate whether feature_flag_id exists and request attributes validation
    *                 * same as create feature flag method
    *               - delete feature flag method deletes featureflag entry
    *                 * delete feature_flag_id if exists
    *
    *         - featureflagrolloutservice
    *               - rollout feature_flag_id with attributes and percentage, with sticky nature ()
    *
    * utils -
    *
    *
    *
    *
    * */
}
