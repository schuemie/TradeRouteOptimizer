import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Class for processing the EDDB data and computing optimal trade routes.
 */
public class DataProcessor implements Serializable {

	public static double							MAX_DISTANCE		= 100;														// Maximum single hop
																																	// distance, used during
																																	// pre-computing
	public static double							MIN_PROFIT			= 500;														// Minimum profit per unit
																																	// per hop
	public static int								BIG_NUMBER			= 999999;													// A big number (should be
																																	// higher than the highest
																																	// buy price)
	public static int								MAX_EXPECTED_PROFIT	= 2100;													// Maximum profit per unit
																																	// per hop expected to be
																																	// seen anywhere in the
																																	// galaxy
	public static int								MIN_SUPPLY			= 100;														// Minimum supply at a
																																	// station for considering
																																	// to buy
	public static int								MIN_DEMAND			= 100;														// Minimum demand at a
																																	// station for considering
																																	// to sell
	public static final int							S					= 1;
	public static final int							M					= 2;
	public static final int							UNKNOWN				= 3;
	public static final int							L					= 4;

	public static String[]							IGNORE_COMMODITIES	= new String[] { "Slaves", "Imperial Slaves", "Tobacco" };
	private static final long						serialVersionUID	= -3242122460841786958L;

	public System[]									systems;
	public Commodity[]								commodities;
	public Station[]								stations;
	public Map<String, System>						nameToSystem;
	private Map<Integer, DistancesAndBestTrades>	stationIdToDistancesAndBestTrades;
	private int										maxProfit;

	private long									hopCounter;

	public static void main(String[] args) {
		DataProcessor dataProcessor = new DataProcessor("s:/temp");
		dataProcessor.saveToFile("s:/temp/EdDataProcessor.bin");
	}

	public void findMultiHops(MultiHopSettings settings) {
		long start = java.lang.System.currentTimeMillis();
		int level = settings.numerOfHops;
		Solution bestSolution = new Solution(settings.numerOfHops);
		Solution workingSolution = new Solution(settings.numerOfHops);
		hopCounter = 0;
		recursiveSearch(level, settings.startStation.id, workingSolution, bestSolution, settings);
		DecimalFormat formatter = new DecimalFormat("###,###,###,###,##0");
		java.lang.System.out.println("Search took " + formatter.format(java.lang.System.currentTimeMillis() - start) + " ms, evaluated "
				+ formatter.format(hopCounter) + " hops");
		bestSolution.print(settings);
	}

	private void recursiveSearch(int level, int stationId, Solution workingSolution, Solution bestSolution, MultiHopSettings settings) {
		if (level == 0) {
			if (workingSolution.profit > bestSolution.profit)
				if (!settings.noLoops || !hasLoop(workingSolution)) // Faster to check at the end than at every hop
					if (settings.viaSystem == null || hasSystem(workingSolution, settings.viaSystem)) {
						bestSolution.copy(workingSolution);
						// java.lang.System.out.println(bestSolution.profit);
					}
		} else {
			if (settings.abort)
				return;
			int profitSoFar = workingSolution.profit;
			DistancesAndBestTrades distancesAndBestTrades = stationIdToDistancesAndBestTrades.get(stationId);
			for (int i = 0; i < distancesAndBestTrades.distances.length; i++) {
				hopCounter++;
				if (profitSoFar + (level - 1) * maxProfit + distancesAndBestTrades.profits[i] < bestSolution.profit) // Already impossible to improve on best
					return;
				if (distancesAndBestTrades.distances[i] > settings.maxHopDistance)
					continue;
				Station station = stations[distancesAndBestTrades.stationIds[i]];
				if (station.distanceToStar > settings.maxDistanceFromStar)
					continue;
				if (station.maxLandingPadSize < settings.requiredLandingPadSize)
					continue;
				workingSolution.stationIds[level - 1] = station.id;
				workingSolution.profit = profitSoFar + distancesAndBestTrades.profits[i];
				recursiveSearch(level - 1, station.id, workingSolution, bestSolution, settings);
			}
		}
	}

	private boolean hasSystem(Solution solution, System system) {
		for (int i = 0; i < solution.stationIds.length - 1; i++)
			if (stations[solution.stationIds[i]].systemId == system.id)
				return true;
		return false;
	}

	private boolean hasLoop(Solution solution) {
		for (int i = 0; i < solution.stationIds.length - 1; i++)
			for (int j = i + 1; j < solution.stationIds.length; j++)
				if (solution.stationIds[i] == solution.stationIds[j])
					return true;
		return false;
	}

	private Trade findBestTrade(Station station1, Station station2, Set<Integer> ignoreCommodityIds) {
		int bestCommodityId = 0;
		int maxProfit = 0;
		for (int i = 0; i < commodities.length; i++)
			if (!ignoreCommodityIds.contains(i)) {
				int profit = station2.sellPrices[i] - station1.buyPrices[i];
				if (profit > maxProfit) {
					maxProfit = profit;
					bestCommodityId = i;
				}
			}
		if (maxProfit > MAX_EXPECTED_PROFIT)
			maxProfit = MAX_EXPECTED_PROFIT;
		Trade trade = new Trade();
		trade.commodityId = bestCommodityId;
		trade.profit = maxProfit;
		return trade;
	}

	private class Trade {
		public int	commodityId;
		public int	profit;

	}

	public System findSystem(String systemName) {
		System system = nameToSystem.get(systemName);
		if (system == null)
			throw new RuntimeException("System " + systemName + " not found");
		return system;
	}

	public Station findStation(String systemName, String stationName) {
		System system = findSystem(systemName);
		if (system.stationIds == null)
			throw new RuntimeException("System " + systemName + " has no stations");
		for (int stationId : system.stationIds)
			if (stations[stationId].name.equals(stationName))
				return stations[stationId];
		throw new RuntimeException("Station " + stationName + " not found in system " + systemName);
	}

	private class Solution {
		int[]	stationIds;
		int[]	commodityIds;
		int		profit;

		public Solution(int size) {
			stationIds = new int[size];
			commodityIds = new int[size];
			profit = 0;
		}

		public void print(MultiHopSettings settings) {
			if (profit == 0) {
				java.lang.System.out.println("No solution matched your criteria");
				return;
			}
			Station startStation = settings.startStation;
			System startSystem = systems[startStation.systemId];
			java.lang.System.out.printf("\nSystem: %s, station: %s\n", startSystem.name, startStation.name);
			double totalDistance = 0;
			int previousStationId = startStation.id;
			for (int i = stationIds.length - 1; i >= 0; i--) {
				Station station = stations[stationIds[i]];
				System system = systems[station.systemId];
				Commodity commodity = null;
				double distance = 0;
				DistancesAndBestTrades distancesAndBestTrades = stationIdToDistancesAndBestTrades.get(previousStationId);
				for (int j = 0; j < distancesAndBestTrades.stationIds.length; j++) {
					if (distancesAndBestTrades.stationIds[j] == station.id) {
						commodity = commodities[distancesAndBestTrades.commodityIds[j]];
						distance = distancesAndBestTrades.distances[j];
						break;
					}
				}
				totalDistance += distance;
				int buyPrice = stations[previousStationId].buyPrices[commodity.id];
				int sellPrice = station.sellPrices[commodity.id];
				java.lang.System.out.printf(" - Buy %s at %,d\n", commodity.name, buyPrice);
				java.lang.System.out.printf("\n-- %.1f LY -->\n", distance);
				java.lang.System.out.printf("System: %s, station: %s (%,d ls, Pad: %s)\n", system.name, station.name, station.distanceToStar,
						encodeLandingPadSize(station.maxLandingPadSize));
				java.lang.System.out.printf(" - Sell %s at %,d, profit: %,d\n", commodity.name, sellPrice, (sellPrice - buyPrice));
				previousStationId = station.id;
			}
			java.lang.System.out.printf("\nTotal profit per unit: %,d, total distance: %,.1f LY\n", profit, totalDistance);
			if (settings.abort)
				java.lang.System.out.println("Warning! User aborted the search, so solution is probably suboptimal");
		}

		public void copy(Solution other) {
			stationIds = other.stationIds.clone();
			commodityIds = other.commodityIds.clone();
			profit = other.profit;

		}
	}

	public static DataProcessor loadFromFile(String filename) {
		try {
			FileInputStream fileInputStream = new FileInputStream(filename);
			ObjectInputStream objectinputstream = new ObjectInputStream(fileInputStream);
			DataProcessor dataProcessor = (DataProcessor) objectinputstream.readObject();
			objectinputstream.close();
			return dataProcessor;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void saveToFile(String filename) {
		java.lang.System.out.println("Writing preprocessed data to disk");
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(filename);
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
			objectOutputStream.writeObject(this);
			objectOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		java.lang.System.out.println("Done writing to disk");
	}

	public DataProcessor(String folder) {
		loadCommodities(folder);
		loadSystems(folder);
		loadStations(folder);
		computeDistancesAndBestTrades();
		java.lang.System.out.println("Preprocessing complete");
	}

	private void computeDistancesAndBestTrades() {
		java.lang.System.out.println("Computing distances and best trades");
		Set<Integer> ignoreCommodityIds = new HashSet<Integer>();
		for (String ignoreCommodity : IGNORE_COMMODITIES) {
			for (Commodity commodity : commodities)
				if (commodity != null && commodity.name.equals(ignoreCommodity))
					ignoreCommodityIds.add(commodity.id);
		}
		stationIdToDistancesAndBestTrades = new HashMap<Integer, DataProcessor.DistancesAndBestTrades>();
		long targetCount = 0;
		int sourceCount = 0;
		maxProfit = 0;
		int sourceSystemCount = 0;
		for (System system1 : systems) {
			sourceSystemCount++;
			if (sourceSystemCount % 1000 == 0)
				java.lang.System.out.println(sourceSystemCount + " of " + systems.length + " systems");
			if (system1 != null && system1.stationIds != null) {
				for (int stationId1 : system1.stationIds) {
					List<DistanceStationId> distanceStationIds = new ArrayList<DataProcessor.DistanceStationId>();
					for (System system2 : systems)
						if (system2 != null && system2.stationIds != null) {
							double distance = distance(system1, system2);
							if (distance <= MAX_DISTANCE) {
								for (int stationId2 : system2.stationIds)
									if (stationId1 != stationId2) {
										Trade trade = findBestTrade(stations[stationId1], stations[stationId2], ignoreCommodityIds);
										if (trade.profit >= MIN_PROFIT) {
											DistanceStationId distanceSystemId = new DistanceStationId();
											distanceSystemId.distance = distance;
											distanceSystemId.stationId = stationId2;
											distanceSystemId.profit = trade.profit;
											distanceSystemId.commodityId = trade.commodityId;
											distanceStationIds.add(distanceSystemId);
											if (trade.profit > maxProfit)
												maxProfit = trade.profit;
										}
									}
							}
						}
					Collections.sort(distanceStationIds);
					DistancesAndBestTrades distances = new DistancesAndBestTrades(distanceStationIds.size());
					for (int i = 0; i < distanceStationIds.size(); i++) {
						distances.distances[i] = distanceStationIds.get(i).distance;
						distances.stationIds[i] = distanceStationIds.get(i).stationId;
						distances.commodityIds[i] = distanceStationIds.get(i).commodityId;
						distances.profits[i] = distanceStationIds.get(i).profit;
					}
					stationIdToDistancesAndBestTrades.put(stationId1, distances);
					targetCount += distanceStationIds.size();
					sourceCount++;
				}
			}
		}
		java.lang.System.out.println("Possible target stations: " + targetCount + ", source stations: " + sourceCount + ", average targets per source: "
				+ (targetCount / (double) sourceCount));
		java.lang.System.out.println("Max profit seen: " + maxProfit);
	}

	private class DistanceStationId implements Comparable<DistanceStationId> {
		public double	distance;
		public int		stationId;
		public int		profit;
		public int		commodityId;

		@Override
		public int compareTo(DistanceStationId o) {
			// return Double.compare(distance, o.distance);
			return Double.compare(o.profit, profit);
		}

	}

	private double distance(System system1, System system2) {
		return Math.sqrt(sqr(system1.x - system2.x) + sqr(system1.y - system2.y) + sqr(system1.z - system2.z));
	}

	private double sqr(double x) {
		return x * x;
	}

	private void loadStations(String folder) {
		java.lang.System.out.println("Loading station data from disk");
		JSONArray jsonStations = loadJSON(folder + "/stations.json");
		int maxId = 0;
		for (int i = 0; i < jsonStations.size(); i++) {
			JSONObject jsonStation = (JSONObject) jsonStations.get(i);
			int id = ((Number) jsonStation.get("id")).intValue();
			if (id > maxId)
				maxId = id;
		}
		stations = new Station[maxId + 1];
		for (int i = 0; i < jsonStations.size(); i++) {
			JSONObject jsonStation = (JSONObject) jsonStations.get(i);
			Station station = new Station();
			station.systemId = ((Number) jsonStation.get("system_id")).intValue();
			station.name = (String) jsonStation.get("name");
			// if (station.name.equals("Mendeleev Gateway"))
			// java.lang.System.out.println("adsf");
			station.id = ((Number) jsonStation.get("id")).intValue();
			Object distanceToStar = jsonStation.get("distance_to_star");
			if (distanceToStar != null)
				station.distanceToStar = ((Number) distanceToStar).intValue();
			String maxLandingPadSize = (String) jsonStation.get("max_landing_pad_size");
			if (maxLandingPadSize == null)
				station.maxLandingPadSize = UNKNOWN;
			else
				station.maxLandingPadSize = parseLandingPadSize(maxLandingPadSize);

			System system = systems[station.systemId];
			if (system.stationIds == null) {
				system.stationIds = new int[] { station.id };
			} else {
				int[] temp = new int[system.stationIds.length + 1];
				java.lang.System.arraycopy(system.stationIds, 0, temp, 0, system.stationIds.length);
				temp[temp.length - 1] = station.id;
				system.stationIds = temp;
			}
			station.buyPrices = new int[commodities.length];
			Arrays.fill(station.buyPrices, BIG_NUMBER);
			station.sellPrices = new int[commodities.length];
			Arrays.fill(station.sellPrices, 0);
			JSONArray jsonListings = (JSONArray) jsonStation.get("listings");
			for (int j = 0; j < jsonListings.size(); j++) {
				JSONObject jsonListing = (JSONObject) jsonListings.get(j);
				int commodityId = ((Number) jsonListing.get("commodity_id")).intValue();
				int supply = ((Number) jsonListing.get("supply")).intValue();
				if (supply >= MIN_SUPPLY)
					station.buyPrices[commodityId] = ((Number) jsonListing.get("buy_price")).intValue();
				if (station.buyPrices[commodityId] == 0)
					station.buyPrices[commodityId] = BIG_NUMBER;
				int demand = ((Number) jsonListing.get("demand")).intValue();
				if (demand >= MIN_DEMAND)
					station.sellPrices[commodityId] = ((Number) jsonListing.get("sell_price")).intValue();
			}
			stations[station.id] = station;
		}
	}

	private int parseLandingPadSize(String s) {
		switch (s) {
			case "S":
				return S;
			case "M":
				return M;
			case "L":
				return L;
		}
		throw new RuntimeException("Unknown landing pad size: " + s);
	}

	private String encodeLandingPadSize(int landingPadSize) {
		switch (landingPadSize) {
			case S:
				return "S";
			case M:
				return "M";
			case L:
				return "L";
			case UNKNOWN:
				return "?";
		}
		throw new RuntimeException("Unknown landing pad size: " + landingPadSize);
	}

	private void loadCommodities(String folder) {
		java.lang.System.out.println("Loading commodity data from disk");
		JSONArray jsonCommodities = loadJSON(folder + "/commodities.json");
		int maxId = 0;
		for (int i = 0; i < jsonCommodities.size(); i++) {
			JSONObject jsonCommoditiy = (JSONObject) jsonCommodities.get(i);
			int id = ((Number) jsonCommoditiy.get("id")).intValue();
			if (id > maxId)
				maxId = id;
		}
		commodities = new Commodity[maxId + 1];
		for (int i = 0; i < jsonCommodities.size(); i++) {
			JSONObject jsonCommoditiy = (JSONObject) jsonCommodities.get(i);
			Commodity commodity = new Commodity();
			commodity.id = ((Number) jsonCommoditiy.get("id")).intValue();
			commodity.name = (String) jsonCommoditiy.get("name");
			commodities[commodity.id] = commodity;
		}
	}

	private void loadSystems(String folder) {
		java.lang.System.out.println("Loading system data from disk");
		JSONArray jsonSystems = loadJSON(folder + "/systems.json");
		int maxId = 0;
		for (int i = 0; i < jsonSystems.size(); i++) {
			JSONObject jsonSystem = (JSONObject) jsonSystems.get(i);
			int id = ((Number) jsonSystem.get("id")).intValue();
			if (id > maxId)
				maxId = id;
		}
		systems = new System[maxId + 1];
		nameToSystem = new HashMap<String, DataProcessor.System>(jsonSystems.size());
		for (int i = 0; i < jsonSystems.size(); i++) {
			JSONObject jsonSystem = (JSONObject) jsonSystems.get(i);
			System system = new System();
			system.x = ((Number) jsonSystem.get("x")).doubleValue();
			system.y = ((Number) jsonSystem.get("y")).doubleValue();
			system.z = ((Number) jsonSystem.get("z")).doubleValue();
			system.id = ((Number) jsonSystem.get("id")).intValue();
			system.name = (String) jsonSystem.get("name");
			systems[system.id] = system;
			nameToSystem.put(system.name, system);
		}
	}

	private static JSONArray loadJSON(String filename) {
		try {
			JSONParser parser = new JSONParser();
			FileInputStream inputStream = new FileInputStream(filename);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
			Object obj = parser.parse(bufferedReader);
			return (JSONArray) obj;
		} catch (Exception e) {

		}
		return null;
	}

	public class System implements Serializable {
		private static final long	serialVersionUID	= -4632007775595080794L;

		public int					id;
		public double				x, y, z;
		public String				name;
		public int[]				stationIds;
	}

	public class Station implements Serializable {
		private static final long	serialVersionUID	= 3297959707912126038L;
		public int					id;
		public String				name;
		public int					systemId;
		public int					distanceToStar;
		public int					maxLandingPadSize;
		public int[]				buyPrices;
		public int[]				sellPrices;
	}

	public class Commodity implements Serializable {
		private static final long	serialVersionUID	= -7095466617130863399L;
		public int					id;
		public String				name;
	}

	public class DistancesAndBestTrades implements Serializable {
		private static final long	serialVersionUID	= -2436412544760811223L;
		public double[]				distances;
		public int[]				stationIds;
		public int[]				profits;
		public int[]				commodityIds;

		public DistancesAndBestTrades(int size) {
			distances = new double[size];
			stationIds = new int[size];
			profits = new int[size];
			commodityIds = new int[size];
		}
	}

	public static class MultiHopSettings {
		public Station	startStation;
		public boolean	abort					= false;
		public int		numerOfHops				= 6;
		public double	maxHopDistance			= 70;
		public int		maxDistanceFromStar		= 1000;
		public int		requiredLandingPadSize	= M;
		public boolean	noLoops					= false;
		public System	viaSystem;
	}
}
