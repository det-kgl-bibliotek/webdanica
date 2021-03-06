package dk.kb.webdanica.core.datamodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Iterator;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import dk.kb.webdanica.core.datamodel.dao.HBasePhoenixConnectionManager;
import dk.kb.webdanica.core.datamodel.dao.HBasePhoenixSeedsDAO;

@RunWith(JUnit4.class)
public class TestHBasePhoenixSeedsDAO {

	@Test
	public void test_hbasephoenix_seeds_dao() {
		HBasePhoenixConnectionManager.register();

		Connection conn = null;
		Properties connprops = new Properties();

		try {
			conn = DriverManager.getConnection( "jdbc:phoenix:localhost", connprops );

			Seed seed = new Seed("http://www.kb.dk/");

			HBasePhoenixSeedsDAO dao = new HBasePhoenixSeedsDAO();
			dao.insertSeed(seed);

			seed.setStatus(Status.NEW);
			seed.setStatusReason("Just added.");
			dao.updateSeed(seed);

			seed.setRedirectedUrl("http://www.karburator.dk/");
			dao.updateSeed(seed);

			long cnt = dao.getSeedsCount(Status.NEW);
			// debug
			System.out.println(cnt);

			cnt = dao.getSeedsCount(Status.AWAITS_CURATOR_DECISION);
			// debug
			System.out.println(cnt);

			Iterator<Seed> seedList = dao.getSeedsForStatus(Status.NEW, 0, 100000);
			while (seedList.hasNext()) {
				seed = seedList.next();
				System.out.println(seed.getUrl());
			}

			conn.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		HBasePhoenixConnectionManager.deregister();
	}

}
