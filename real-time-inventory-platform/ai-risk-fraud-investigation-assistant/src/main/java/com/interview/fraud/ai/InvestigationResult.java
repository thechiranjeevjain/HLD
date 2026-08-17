package com.interview.fraud.ai;
import java.util.List;
public record InvestigationResult(String summary,String riskLevel,int deterministicScore,List<String> keyRiskSignals,List<Evidence> evidence,List<PolicyRagService.Citation> policyCitations,List<String> missingInformation,String recommendedAction,double confidence,List<String> assumptions,boolean humanApprovalRequired){public record Evidence(String source,String finding){} }
