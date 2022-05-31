import React, { useState, useCallback } from "react";
import Layout from "@theme/Layout";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import "./index.scss";
import { COUNTRIES } from "../../components/selector/countries";
import { useLocation, useHistory } from "@docusaurus/router";
import axios from "axios";
import { useForm } from "react-hook-form";

function ContactUs() {
  const { siteConfig = {} } = useDocusaurusContext();
  const { customFields = {} } = siteConfig;
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(undefined);
  const histroy = useHistory();

  const onSubmit = async (values) => {
    setLoading(true);
    setError(undefined);
    try {
      const resp = await axios.post("/api/v1/contact", values);
      histroy.push(siteConfig.baseUrl + "/notice?type=success");
    } catch (errorList) {
      setError(errorList);
      errorList.forEach(({ name, errors }) => {});
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout
      title={`Contact Us`}
      description="Contact Us For More Information about Starwhale"
    >
      <main className="swContact">
        <section className="contact">
          <h1>Contact Us</h1>
          <form onSubmit={handleSubmit(onSubmit)}>
            <section className="contact__body">
              <div className="form">
                <label className="block">
                  <span className="">First Name *</span>
                  <input
                    placeholder="your first name"
                    {...register("firstName", { required: true })}
                  />
                </label>
                <label className="block">
                  <span className="">Last Name *</span>
                  <input
                    placeholder="your last name"
                    {...register("lastName", { required: true })}
                  />
                </label>
                <label className="block">
                  <span className="">Company Name *</span>
                  <input
                    placeholder="your company name"
                    {...register("companyName", { required: true })}
                  />
                </label>
                <label className="block">
                  <span>Email *</span>
                  <input
                    type="email"
                    placeholder="name@work-email.com"
                    {...register("email", { required: true })}
                  />
                </label>
                <label className="block">
                  <span>Country *</span>
                  <div className="selector">
                    <select {...register("countryAbb", { required: true })}>
                      <option value="">select country</option>
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
                  <span>Message</span>
                  <textarea rows="3" {...register("message")} />
                </label>
                {error && (
                  <label className="block">
                    <span></span>
                    <span
                      style={{ color: "red", textAlign: "right", flex: "1" }}
                    >
                      {error?.message + ",Try Later"}
                    </span>
                  </label>
                )}
                <div className="block">
                  <div style={{ flex: 1 }}></div>
                  <button
                    type="submit"
                    className="button button--primary button--rounded"
                    disabled={loading}
                  >
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
          </form>
        </section>
      </main>
    </Layout>
  );
}

export default ContactUs;
