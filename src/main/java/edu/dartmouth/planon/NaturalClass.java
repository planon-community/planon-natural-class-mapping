package edu.dartmouth.planon;

import java.util.HashMap;
import java.util.Map;

import nl.planon.hades.userextension.uxinterface.*;
import nl.planon.util.pnlogging.PnLogger;

// Class to get the natural class given
public class NaturalClass implements IUserExtension{

    private static final PnLogger LOG = PnLogger.getLogger(NaturalClass.class);

    // Set description
    private String description = "This solution extension returns natural class";

    public NaturalClass() {}

    public void executeUX (IUXBusinessObject currentBO, IUXBusinessObject oldBO, IUXContext context, String parameter) {
        LOG.info("--------------------------------------------");
        LOG.info("SX " + getClass().getName() + " starting..." + NaturalClass.class.getName());

        Map<String, String> parameterMap = getParameterAsStringList(parameter,"=");

        // System.out.println(parameterMap.get("chartString"));
        LOG.info(parameterMap);

        // Order lines
        // Get the chart string
        String chartString = currentBO.getStringFieldByName(parameterMap.get("chartString")).getValueAsString();
        LOG.info("Chartstring: " + chartString);

        // Get the cost type
        String costType = currentBO.getCodesCodeNameFieldByName(parameterMap.get("costType")).getValueAsString();
        LOG.info("Cost type: " + costType);

        // Get the work order method
        String woMethod = currentBO.getCodesNameFieldByName(parameterMap.get("woMethod")).getValueAsString();
        LOG.info("WO method: " + woMethod);

        // Get the trade (crew) via Order BO
        IUXReferenceField orderReferenceField = currentBO.getReferenceFieldByName("OrderRef");
        IUXBusinessObject order = orderReferenceField.getValueAsBO();
        IUXReferenceField tradeReferenceField = order.getReferenceFieldByName("TradeRef");
        IUXBusinessObject trade = tradeReferenceField.getValueAsBO();
        Integer tradePrimaryKey = trade.getPrimaryKey();
        LOG.info("Trade Primary Key: " + tradePrimaryKey);

        // Get entity
        String entity = chartString.substring(0,2);
        Integer entityInt = Integer.parseInt(entity);
        if (entityInt < 50){
            entity = "Internal < 50";
        } else{
            entity = "External ≥ 50";
        }
        LOG.info("Entity: " + entity);

        // Get org
        String org = chartString.substring(3, 6);
        LOG.info("Org: " + org);
        if (!org.equals("003")){
            org = "All else";
        }
        LOG.info("Org: " + org);

        // Get natural classes from the mapping table
        String[] naturalClasses = calculateNaturalClass(woMethod, costType, tradePrimaryKey, entity, org, context, parameterMap);
        LOG.info("Natural Classes: " + naturalClasses);
        String naturalClassExpense = naturalClasses[0];
        String naturalClassRevenue = naturalClasses[1];
        LOG.info("Natural Class Expense: " + naturalClassExpense);
        LOG.info("Natural Class Revenue: " + naturalClassRevenue);

        // Set Natural chart strings on order lines
        IUXStringField nChartStringExpense = currentBO.getStringFieldByName(parameterMap.get("chartStringExpense"));
        nChartStringExpense.setValueAsString(chartString + '.' + naturalClassExpense);
        IUXStringField nChartStringRevenue = currentBO.getStringFieldByName(parameterMap.get("chartStringRevenue"));
        nChartStringRevenue.setValueAsString(chartString + '.' + naturalClassRevenue);

        // Save changes
        currentBO.executeFieldChanges();
    }

    public String[] calculateNaturalClass (String woMethod, String costType, Integer tradePrimaryKey, String entity, String org, IUXContext context, Map<String, String> parameterMap){
        // Calculate natural class

        // Find the UsrNaturalClassMapping BO
        IUXDatabaseQueryBuilder queryBldr = context.getBODatabaseQueryBuilder("UsrNaturalClassMapping");
        queryBldr.addSelectField("Syscode");
        queryBldr.addSearchField(parameterMap.get("trade")); // Trade
        queryBldr.addSearchField(parameterMap.get("org")); // Org
        queryBldr.addSearchField(parameterMap.get("entity")); // Entity
        queryBldr.addSearchField(parameterMap.get("NCwoMethod")); // Work order method
        queryBldr.addSearchField(parameterMap.get("NCcostType")); // Cost type
        IUXDatabaseQuery query = queryBldr.build();
        query.getIntegerSearchExpression(parameterMap.get("trade"), UXOperator.EQUAL).setValue(tradePrimaryKey);
        query.getStringSearchExpression(parameterMap.get("org"), UXOperator.EQUAL).setValue(org);
        query.getStringSearchExpression(parameterMap.get("entity"), UXOperator.EQUAL).setValue(entity);
        query.getStringSearchExpression(parameterMap.get("NCwoMethod"), UXOperator.EQUAL).setValue(woMethod);
        query.getStringSearchExpression(parameterMap.get("NCcostType"), UXOperator.EQUAL).setValue(costType);
        IUXResultSet result = query.executeAll();
        String naturalClassExpense, naturalClassRevenue;
        naturalClassExpense = naturalClassRevenue = parameterMap.get("naturalClassDefault");

        if (result.first()) {
            Integer usrNaturalClassMappingRef = result.getPrimaryKey();
            IUXBusinessObject usrNaturalClassMapping = context.getBOByPrimaryKey("UsrNaturalClassMapping", usrNaturalClassMappingRef);
            naturalClassExpense = usrNaturalClassMapping.getStringFieldByName(parameterMap.get("naturalClassExpense")).getValueAsString();
            naturalClassRevenue = usrNaturalClassMapping.getStringFieldByName(parameterMap.get("naturalClassRevenue")).getValueAsString();
        }

        String[] naturalClass = new String[2];
        naturalClass[0] = naturalClassExpense;
        naturalClass[1] = naturalClassRevenue;
        return naturalClass;
    }

    // Convert string parameter to list
    public static Map<String, String> getParameterAsStringList(String parameter, String delimiter) {
        Map<String, String> myMap = new HashMap<String, String>();
        // Split each line to pairs
        String[] pairs = parameter.split("\n");
        LOG.info(pairs);
        // Loop through each pair
        for (int i=0; i<pairs.length; i++) {
            String pair = pairs[i];
            LOG.info(pair);
            String[] keyValue = pair.split(delimiter);
            LOG.info(keyValue);
            LOG.info(keyValue[0]);
            LOG.info(keyValue[1]);
            myMap.put(keyValue[0], keyValue[1]);
        }
        return myMap;
    }

    // Return description
    public String getDescription() {
        return description;
    }
}
