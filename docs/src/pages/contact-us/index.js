import React, { useState } from "react";
import Layout from "@theme/Layout";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import "./index.scss";
import { COUNTRIES } from "../../components/selector/countries";

function ContactUs() {
  const { siteConfig = {} } = useDocusaurusContext();
  const { customFields = {} } = siteConfig;

  return (
    <Layout
      title={`Contact Us`}
      description="Contact Us For More Information about Starwhale"
    >
      <main className="swContact">
        <section className="contact">
          <h1>Contact Us</h1>
          <section className="contact__body">
            <div className="form">
              <label className="block">
                <span className="">First Name *</span>
                <input type="text" placeholder="your first name" />
              </label>
              <label className="block">
                <span className="">Last Name *</span>
                <input type="text" placeholder="your last name" />
              </label>
              <label className="block">
                <span className="">Company Name *</span>
                <input type="text" placeholder="your company name" />
              </label>
              <label className="block">
                <span className=" ">Email *</span>
                <input type="email" placeholder="name@work-email.com" />
              </label>
              <label className="block">
                <span className=" ">Country *</span>
                <div className="selector">
                  <select>
                    <option>select country</option>
                    {COUNTRIES.map((country, index) => {
                      return (
                        <option key={index} value={country.abb}>
                          {country.name}
                        </option>
                      );
                    })}
                  </select>
                </div>
              </label>
              <label className="block">
                <span className=" ">Message</span>
                <textarea rows="3"></textarea>
              </label>
              <div className="block">
                <div style={{ flex: 1 }}></div>
                <button className="button button--primary button--rounded">
                  Submit
                </button>
              </div>
            </div>

            <section className="notice">
              <h2>We'd love to know what we can support </h2>
              <div className="notice__divider"></div>
              <div className="notice__body">email: {customFields.email}</div>
            </section>
          </section>
        </section>
      </main>
    </Layout>
  );
}

export default ContactUs;
