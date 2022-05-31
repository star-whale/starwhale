import React, { useState, useCallback } from "react";
import Layout from "@theme/Layout";
import useDocusaurusContext from "@docusaurus/useDocusaurusContext";
import "./index.scss";
import { useLocation, useHistory } from "@docusaurus/router";
import useBaseUrl from "@docusaurus/useBaseUrl";

const notices = {
  success: {
    icon: "img/success.svg",
    text: (
      <>
        We have received the message, thanks for contacting.
        <p>We'll get back toyou shortly!</p>
      </>
    ),
  },
};

function Notice() {
  const { siteConfig = {} } = useDocusaurusContext();
  const { customFields = {} } = siteConfig;
  const location = useLocation();
  const history = useHistory();

  const info = notices[location.search] ?? notices["success"];

  setTimeout(() => {
    history.push(siteConfig.baseUrl);
  }, 3000);

  return (
    <Layout title={siteConfig.title} description={customFields.description}>
      <main className="swNotice">
        <section className="notice">
          <img src={useBaseUrl(info.icon)} alt="success" height={80} />
          <div className="notice__body">{info.text}</div>
          <div className="notice__return">
            We will return back to homepage shortly
          </div>
        </section>
      </main>
    </Layout>
  );
}

export default Notice;
